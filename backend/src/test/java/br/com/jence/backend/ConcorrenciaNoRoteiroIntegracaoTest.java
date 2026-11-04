package br.com.jence.backend;

import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.service.RemoverProdutoDoRoteiroUseCase;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Duas requisicoes mexendo no mesmo roteiro ao mesmo tempo.
 * <p>
 * O cenario nao e exotico: o cliente pega dois produtos do mesmo corredor e toca em "coletei"
 * nos dois seguidos; ou o totem fica aberto enquanto o celular caminha; ou dois aparelhos
 * usam a mesma sessao, o que a regeneracao de QR (D-44) passou a permitir.
 * <p>
 * <b>Chama os casos de uso, nao os repositorios</b> - e o caminho que uma requisicao HTTP
 * percorre de verdade. O nivel importa: a gravacao pelo repositorio, isolada, tem uma
 * propriedade destrutiva que nenhum caso de uso exercita (ver D-48).
 * <p>
 * As chamadas partem juntas de uma barreira para maximizar a sobreposicao. Vale saber que
 * <b>corrida nao e prova</b>: este teste protege contra regressao, mas quem demonstra o
 * comportamento de forma reproduzivel e o {@code EscritaObsoletaIntegracaoTest}.
 */
@Tag("integracao")
@SpringBootTest
class ConcorrenciaNoRoteiroIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired MarcarItemColetadoUseCase marcarColetado;
    @Autowired RemoverProdutoDoRoteiroUseCase remover;
    private final ListaRoteiroDao listaRoteiroDao = BancoDeTeste.listas();
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();


    private UUID sessaoId;

    @BeforeEach
    void montarRoteiro() throws Exception {
        sessaoId = inicializar.executar(null).id();
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

    private ListaRoteiro carregar() throws Exception {
        return listaRoteiroDao.buscarPorSessao(sessaoId).orElseThrow();
    }

    private List<ItemRoteiro> itensEmOrdemEstavel(ListaRoteiro lista) {
        return lista.getItens().stream()
                .sorted(Comparator.comparing(i -> i.getProduto().getSku()))
                .toList();
    }

    private void imprimir(String titulo, ListaRoteiro lista) {
        System.out.println(">>> " + titulo);
        itensEmOrdemEstavel(lista).forEach(i -> System.out.printf("    %-12s coletado=%s%n",
                i.getProduto().getSku(), i.isColetado()));
    }

    /** Dispara as acoes juntas e espera todas terminarem, propagando qualquer falha. */
    /** Uma acao que pode lancar excecao checada, como os casos de uso que ja usam JDBC. */
    private interface Acao {
        void executar() throws Exception;
    }

    private void emParalelo(Acao... acoes) throws Exception {
        CyclicBarrier largada = new CyclicBarrier(acoes.length);
        ExecutorService executor = Executors.newFixedThreadPool(acoes.length);
        try {
            List<Future<?>> execucoes = new java.util.ArrayList<>();
            for (Acao acao : acoes) {
                execucoes.add(executor.submit(() -> {
                    largada.await(10, TimeUnit.SECONDS);
                    acao.executar();
                    return null;
                }));
            }

            for (Future<?> execucao : execucoes) {
                execucao.get(30, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }
    }

    // ---------------------------------------------------------------- sonda 1

    @Test
    @DisplayName("dois itens marcados ao mesmo tempo continuam os dois marcados")
    void duasMarcacoesNaoSePerdem() throws Exception {
        List<ItemRoteiro> itens = itensEmOrdemEstavel(carregar());
        UUID primeiro = itens.getFirst().getId();
        UUID segundo = itens.get(1).getId();

        emParalelo(
                () -> marcarColetado.executar(primeiro),
                () -> marcarColetado.executar(segundo));

        ListaRoteiro noBanco = carregar();
        imprimir("estado final no banco", noBanco);

        List<UUID> coletados = itensEmOrdemEstavel(noBanco).stream()
                .filter(ItemRoteiro::isColetado)
                .map(ItemRoteiro::getId)
                .toList();

        // mesma colecao sem olhar a ordem: compara as duas listas ordenadas
        assertEquals(Stream.of(primeiro, segundo).sorted().toList(),
                coletados.stream().sorted().toList(),
                "o cliente marcou os dois; nenhum pode ter voltado atras");
    }

    // ---------------------------------------------------------------- sonda 2

    @Test
    @DisplayName("item adicionado no totem sobrevive a uma marcacao vinda do celular")
    void itemAdicionadoNaoDesaparece() throws Exception {
        UUID paraColetar = itensEmOrdemEstavel(carregar()).getFirst().getId();
        UUID furadeira = produtoDao.buscarPorSku("SKU-FER-001").orElseThrow().getId();

        emParalelo(
                () -> adicionar.executar(sessaoId, furadeira),
                () -> marcarColetado.executar(paraColetar));

        ListaRoteiro noBanco = carregar();
        imprimir("estado final no banco", noBanco);

        assertEquals(4, noBanco.getItens().size(),
                "marcar um item como coletado nao pode APAGAR o que o totem acabou de somar");

        assertTrue(itensEmOrdemEstavel(noBanco).stream()
                .filter(i -> i.getId().equals(paraColetar))
                .findFirst().orElseThrow().isColetado(),
                "e a marcacao tambem precisa sobreviver");
    }

    // ---------------------------------------------------------------- sonda 3

    @Test
    @DisplayName("remover um item nao desfaz a coleta de outro")
    void remocaoNaoDesfazColeta() throws Exception {
        List<ItemRoteiro> itens = itensEmOrdemEstavel(carregar());
        UUID paraColetar = itens.getFirst().getId();
        UUID paraRemover = itens.getLast().getId();

        emParalelo(
                () -> marcarColetado.executar(paraColetar),
                () -> remover.executar(sessaoId, paraRemover));

        ListaRoteiro noBanco = carregar();
        imprimir("estado final no banco", noBanco);

        assertEquals(2, noBanco.getItens().size());
        assertTrue(itensEmOrdemEstavel(noBanco).getFirst().isColetado(),
                "remover um item nao pode desfazer a coleta de outro");
    }

    // ---------------------------------------------------------------- sonda 4

    @Test
    @DisplayName("o roteiro inteiro marcado de uma vez nao perde nenhuma marcacao")
    void marcacaoEmLoteNaoPerdeNada() throws Exception {
        List<ItemRoteiro> itens = itensEmOrdemEstavel(carregar());

        emParalelo(itens.stream()
                .map(item -> (Acao) () -> marcarColetado.executar(item.getId()))
                .toArray(Acao[]::new));

        ListaRoteiro noBanco = carregar();
        imprimir("estado final no banco", noBanco);

        assertTrue(noBanco.getItens().stream().allMatch(ItemRoteiro::isColetado),
                "cliente no mesmo corredor marcando tudo de uma vez");
    }
}
