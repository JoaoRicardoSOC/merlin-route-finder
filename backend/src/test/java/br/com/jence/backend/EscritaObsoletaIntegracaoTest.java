package br.com.jence.backend;

import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uma requisicao que grava depois de outra ter mexido no mesmo roteiro.
 * <p>
 * <b>E o teste que demonstra, de forma reproduzivel, que a escrita pelo agregado e segura</b> -
 * e o unico dos tres que escrevemos capaz de distinguir um comportamento do outro. Corrida com
 * <i>threads</i> nao serve (duas requisicoes contra um banco local raramente se sobrepoem na
 * janela exata) e contar linhas escritas tambem nao (o Hibernate ja nao reescreve irmao que
 * nao mudou).
 * <p>
 * Aqui a sobreposicao e forcada, sem sorte: a operacao de fora roda numa transacao que o teste
 * controla e ainda nao confirmou, enquanto uma segunda operacao roda e confirma numa transacao
 * propria. Quando a de fora finalmente grava, o banco ja mudou embaixo dela.
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
    @Autowired ListaRoteiroRepository listaRoteiroRepository;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired PlatformTransactionManager gerenciadorDeTransacao;

    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;

    private UUID sessaoId;
    private TransactionTemplate transacaoDeFora;
    private TransactionTemplate transacaoParalela;

    @BeforeEach
    void montarRoteiro() {
        sessaoId = inicializar.executar().id();
        adicionarAoRoteiro("SKU-TIN-001");
        adicionarAoRoteiro("SKU-ENC-001");
        adicionarAoRoteiro("SKU-JAR-001");

        transacaoDeFora = new TransactionTemplate(gerenciadorDeTransacao);

        transacaoParalela = new TransactionTemplate(gerenciadorDeTransacao);
        transacaoParalela.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @AfterEach
    void limpar() {
        listaJpa.findBySessaoId(sessaoId).ifPresent(listaJpa::delete);
        sessaoJpa.deleteById(sessaoId);
    }

    private void adicionarAoRoteiro(String sku) {
        adicionar.executar(sessaoId, produtoRepository.buscarPorSku(sku).orElseThrow().getId());
    }

    private List<ItemRoteiro> itens() {
        return listaRoteiroRepository.buscarPorSessao(sessaoId).orElseThrow().getItens().stream()
                .sorted(Comparator.comparing(i -> i.getProduto().getSku()))
                .toList();
    }

    private void imprimir(String titulo) {
        System.out.println(">>> " + titulo);
        itens().forEach(i -> System.out.printf("    %-12s coletado=%s%n",
                i.getProduto().getSku(), i.isColetado()));
    }

    @Test
    @DisplayName("uma marcacao que demora a confirmar nao desfaz outra que confirmou antes")
    void marcacaoLentaNaoDesfazAOutra() {
        List<ItemRoteiro> iniciais = itens();
        UUID lento = iniciais.getFirst().getId();
        UUID rapido = iniciais.get(1).getId();

        transacaoDeFora.executeWithoutResult(status -> {
            // Requisicao lenta: carrega o roteiro e marca o primeiro item...
            marcarColetado.executar(lento);

            // ...e, antes de confirmar, uma segunda requisicao marca outro item e confirma.
            transacaoParalela.executeWithoutResult(interna -> marcarColetado.executar(rapido));

            // Ao sair daqui a requisicao lenta confirma, ja com uma visao desatualizada.
        });

        imprimir("estado final no banco");

        List<UUID> coletados = itens().stream()
                .filter(ItemRoteiro::isColetado)
                .map(ItemRoteiro::getId)
                .toList();

        assertThat(coletados)
                .as("a requisicao lenta nao pode apagar o que a rapida ja tinha gravado")
                .containsExactlyInAnyOrder(lento, rapido);
    }

    @Test
    @DisplayName("uma marcacao que demora a confirmar nao apaga item adicionado nesse intervalo")
    void marcacaoLentaNaoApagaItemNovo() {
        UUID lento = itens().getFirst().getId();
        UUID furadeira = produtoRepository.buscarPorSku("SKU-FER-001").orElseThrow().getId();

        transacaoDeFora.executeWithoutResult(status -> {
            marcarColetado.executar(lento);

            // O totem soma um produto enquanto a marcacao ainda nao confirmou.
            transacaoParalela.executeWithoutResult(interna -> adicionar.executar(sessaoId, furadeira));
        });

        imprimir("estado final no banco");

        assertThat(itens())
                .as("marcar um item nao pode APAGAR o que o totem somou nesse intervalo")
                .hasSize(4);
    }
}
