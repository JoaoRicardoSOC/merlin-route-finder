package br.com.jence.backend;

import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.model.ItemRoteiro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import br.com.jence.backend.factory.Transacao;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Uma requisicao que grava depois de outra ter mexido no mesmo roteiro.
 * <p>
 * <b>E o teste que demonstra, de forma reproduzivel, que a escrita pelo agregado e segura</b> -
 * e o unico dos tres que escrevemos capaz de distinguir um comportamento do outro. Corrida com
 * <i>threads</i> nao serve (duas requisicoes contra um banco local raramente se sobrepoem na
 * janela exata) e contar linhas escritas tambem nao (o Hibernate ja nao reescreve irmao que
 * nao mudou).
 * <p>
 * Aqui a sobreposicao e forcada, sem sorte: a operacao de fora roda numa {@link Transacao} que o
 * teste controla e ainda nao confirmou, enquanto uma segunda operacao roda e confirma numa transacao
 * propria, em outra thread. Quando a de fora finalmente confirma, o banco ja mudou embaixo dela.
 * <p>
 * O resultado esperado - e verificado - e que nada se perde. O motivo esta na D-48: leitura e
 * gravacao acontecem na mesma transacao, entao as entidades estao gerenciadas e o Hibernate so
 * escreve o que mudou em memoria.
 */
@Tag("integracao")
@SpringBootTest
class EscritaObsoletaIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired MarcarItemColetadoUseCase marcarColetado;
    private final ListaRoteiroDao listaRoteiroDao = BancoDeTeste.listas();
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();


    private UUID sessaoId;

    @BeforeEach
    void montarRoteiro() throws Exception {
        sessaoId = inicializar.executar(null).getId();
        adicionarAoRoteiro("SKU-TIN-001");
        adicionarAoRoteiro("SKU-ENC-001");
        adicionarAoRoteiro("SKU-JAR-001");
    }

    @AfterEach
    void limpar() throws Exception {
        BancoDeTeste.apagarSessao(sessaoId);
    }

    private void adicionarAoRoteiro(String sku) throws Exception {
        adicionar.executar(sessaoId, produtoDao.buscarPorSku(sku).orElseThrow().getId());
    }

    private List<ItemRoteiro> itens() throws Exception {
        return listaRoteiroDao.buscarPorSessao(sessaoId).orElseThrow().getItens().stream()
                .sorted(Comparator.comparing(i -> i.getProduto().getSku()))
                .toList();
    }

    private void imprimir(String titulo) throws Exception {
        System.out.println(">>> " + titulo);
        itens().forEach(i -> System.out.printf("    %-12s coletado=%s%n",
                i.getProduto().getSku(), i.isColetado()));
    }

    /** Uma chamada que pode lancar excecao checada. */
    private interface Chamada {
        void executar() throws Exception;
    }

    /**
     * Roda a chamada em outra thread, como uma segunda requisicao HTTP.
     * <p>
     * Precisa ser outra thread: o JDBC grava na hora, e nao no commit como o Hibernate, entao a
     * requisicao lenta ja segura a linha da sessao quando a segunda chega. Numa thread so, a
     * segunda esperaria pela primeira, que esperaria pela segunda. Em threads separadas, a
     * segunda espera a primeira confirmar e segue - que e o que acontece em producao.
     */
    private Future<?> emOutraRequisicao(Chamada chamada) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            return executor.submit(() -> {
                chamada.executar();
                return null;
            });
        } finally {
            executor.shutdown();
        }
    }

    @Test
    @DisplayName("uma marcacao que demora a confirmar nao desfaz outra que confirmou antes")
    void marcacaoLentaNaoDesfazAOutra() throws Exception {
        List<ItemRoteiro> iniciais = itens();
        UUID lento = iniciais.getFirst().getId();
        UUID rapido = iniciais.get(1).getId();

        try (Transacao deFora = Transacao.iniciar()) {
            // Requisicao lenta: carrega o roteiro e marca o primeiro item...
            marcarColetado.executar(lento);

            // ...e, antes de ela confirmar, uma segunda requisicao marca outro item.
            Future<?> paralela = emOutraRequisicao(() -> marcarColetado.executar(rapido));

            // So agora a requisicao lenta confirma, e a segunda termina em seguida.
            deFora.confirmar();
            deFora.close();
            paralela.get(30, TimeUnit.SECONDS);
        }

        imprimir("estado final no banco");

        List<UUID> coletados = itens().stream()
                .filter(ItemRoteiro::isColetado)
                .map(ItemRoteiro::getId)
                .toList();

        // Comparacao sem ordem: as duas listas ordenadas precisam ser identicas.
        List<UUID> esperados = new ArrayList<>(List.of(lento, rapido));
        List<UUID> obtidos = new ArrayList<>(coletados);
        esperados.sort(Comparator.naturalOrder());
        obtidos.sort(Comparator.naturalOrder());
        assertEquals(esperados, obtidos,
                "a requisicao lenta nao pode apagar o que a rapida ja tinha gravado");
    }

    @Test
    @DisplayName("uma marcacao que demora a confirmar nao apaga item adicionado nesse intervalo")
    void marcacaoLentaNaoApagaItemNovo() throws Exception {
        UUID lento = itens().getFirst().getId();
        UUID furadeira = produtoDao.buscarPorSku("SKU-FER-001").orElseThrow().getId();

        try (Transacao deFora = Transacao.iniciar()) {
            marcarColetado.executar(lento);

            // O totem soma um produto enquanto a marcacao ainda nao confirmou.
            Future<?> paralela = emOutraRequisicao(() -> adicionar.executar(sessaoId, furadeira));

            deFora.confirmar();
            deFora.close();
            paralela.get(30, TimeUnit.SECONDS);
        }

        imprimir("estado final no banco");

        assertEquals(4, itens().size(),
                "marcar um item nao pode APAGAR o que o totem somou nesse intervalo");
    }
}
