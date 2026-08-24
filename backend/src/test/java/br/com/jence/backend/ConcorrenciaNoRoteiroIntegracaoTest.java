package br.com.jence.backend;

import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.RemoverProdutoDoRoteiroUseCase;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
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

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

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
    @Autowired ListaRoteiroRepository listaRoteiroRepository;
    @Autowired ProdutoRepository produtoRepository;

    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;

    private UUID sessaoId;

    @BeforeEach
    void montarRoteiro() {
        sessaoId = inicializar.executar().id();
        adicionarAoRoteiro("SKU-TIN-001");
        adicionarAoRoteiro("SKU-ENC-001");
        adicionarAoRoteiro("SKU-JAR-001");
    }

    @AfterEach
    void limpar() {
        listaJpa.findBySessaoId(sessaoId).ifPresent(listaJpa::delete);
        sessaoJpa.deleteById(sessaoId);
    }

    private void adicionarAoRoteiro(String sku) {
        adicionar.executar(sessaoId, produtoRepository.buscarPorSku(sku).orElseThrow().getId());
    }

    private ListaRoteiro carregar() {
        return listaRoteiroRepository.buscarPorSessao(sessaoId).orElseThrow();
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
    private void emParalelo(Runnable... acoes) throws Exception {
        CyclicBarrier largada = new CyclicBarrier(acoes.length);
        ExecutorService executor = Executors.newFixedThreadPool(acoes.length);
        try {
            List<Future<?>> execucoes = new java.util.ArrayList<>();
            for (Runnable acao : acoes) {
                execucoes.add(executor.submit(() -> {
                    largada.await(10, TimeUnit.SECONDS);
                    acao.run();
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

        assertThat(coletados)
                .as("o cliente marcou os dois; nenhum pode ter voltado atras")
                .containsExactlyInAnyOrder(primeiro, segundo);
    }

    // ---------------------------------------------------------------- sonda 2

    @Test
    @DisplayName("item adicionado no totem sobrevive a uma marcacao vinda do celular")
    void itemAdicionadoNaoDesaparece() throws Exception {
        UUID paraColetar = itensEmOrdemEstavel(carregar()).getFirst().getId();
        UUID furadeira = produtoRepository.buscarPorSku("SKU-FER-001").orElseThrow().getId();

        emParalelo(
                () -> adicionar.executar(sessaoId, furadeira),
                () -> marcarColetado.executar(paraColetar));

        ListaRoteiro noBanco = carregar();
        imprimir("estado final no banco", noBanco);

        assertThat(noBanco.getItens())
                .as("marcar um item como coletado nao pode APAGAR o que o totem acabou de somar")
                .hasSize(4);

        assertThat(itensEmOrdemEstavel(noBanco).stream()
                .filter(i -> i.getId().equals(paraColetar))
                .findFirst().orElseThrow().isColetado())
                .as("e a marcacao tambem precisa sobreviver")
                .isTrue();
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

        assertThat(noBanco.getItens()).hasSize(2);
        assertThat(itensEmOrdemEstavel(noBanco).getFirst().isColetado())
                .as("remover um item nao pode desfazer a coleta de outro")
                .isTrue();
    }

    // ---------------------------------------------------------------- sonda 4

    @Test
    @DisplayName("o roteiro inteiro marcado de uma vez nao perde nenhuma marcacao")
    void marcacaoEmLoteNaoPerdeNada() throws Exception {
        List<ItemRoteiro> itens = itensEmOrdemEstavel(carregar());

        emParalelo(itens.stream()
                .map(item -> (Runnable) () -> marcarColetado.executar(item.getId()))
                .toArray(Runnable[]::new));

        ListaRoteiro noBanco = carregar();
        imprimir("estado final no banco", noBanco);

        assertThat(noBanco.getItens())
                .as("cliente no mesmo corredor marcando tudo de uma vez")
                .allMatch(ItemRoteiro::isColetado);
    }
}
