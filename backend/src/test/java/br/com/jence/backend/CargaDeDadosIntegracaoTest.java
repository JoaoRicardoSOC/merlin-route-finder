package br.com.jence.backend;

import br.com.jence.backend.application.usecase.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.seed.CarregadorDadosIniciais;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A massa de demonstracao contra o Oracle real: a carga incremental nao duplica nada, e cada
 * par de substituicao plantado para a demonstracao esta ao alcance da busca espacial.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class CargaDeDadosIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;
    @Autowired PontoMapaRepository pontoMapaRepository;
    @Autowired CarregadorDadosIniciais carregador;

    /** Cada produto e o substituto que a demonstracao espera encontrar quando ele zerar. */
    private static final Map<String, String> PARES_DE_SUBSTITUICAO = Map.of(
            "SKU-TIN-003", "SKU-TIN-004",   // lixa grao 120  -> lixa d'agua grao 150
            "SKU-ILU-001", "SKU-ILU-003",   // lampada LED 9W -> lampada LED 12W
            "SKU-ENC-004", "SKU-ENC-005",   // sifao sanfonado -> sifao copo
            "SKU-FER-002", "SKU-FER-003",   // trena 5m       -> trena 7,5m
            "SKU-MAT-001", "SKU-MAT-003");  // argamassa AC-II -> argamassa AC-III

    private Produto porSku(String sku) {
        return produtoRepository.buscarPorSku(sku).orElseThrow(
                () -> new AssertionError("produto ausente na massa: " + sku));
    }

    @Test
    void todoParDeSubstituicaoEstaAoAlcanceDaBuscaEspacial() {
        PARES_DE_SUBSTITUICAO.forEach((skuEmFalta, skuEsperado) -> {
            Produto emFalta = porSku(skuEmFalta);

            List<String> candidatos = produtoRepository.buscarDisponiveisProximosDe(
                            emFalta.getPontoMapa(), emFalta.getId(),
                            TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA,
                            TratarRupturaEstoqueUseCase.LIMITE_DE_CANDIDATOS)
                    .stream().map(Produto::getSku).toList();

            System.out.printf(">>> %s (%s) -> %d candidato(s), substituto presente: %s%n",
                    skuEmFalta, emFalta.getNome(), candidatos.size(),
                    candidatos.contains(skuEsperado));

            assertThat(candidatos)
                    .as("zerar %s precisa colocar %s entre os candidatos, senao a demonstracao "
                            + "cai em 422 em vez de mostrar a sugestao", skuEmFalta, skuEsperado)
                    .contains(skuEsperado);

            assertThat(candidatos)
                    .as("com um candidato so nao ha escolha semantica a demonstrar em %s", skuEmFalta)
                    .hasSizeGreaterThan(1);
        });
    }

    @Test
    void aCargaIncrementalNaoDuplicaNada() {
        long produtosAntes = produtoRepository.buscarPaginado(0, 1).totalElementos();
        int pontosAntes = todosOsPontos().size();

        // Rodar a carga de novo e exatamente o que acontece a cada reinicio da aplicacao.
        carregador.run(null);

        assertThat(produtoRepository.buscarPaginado(0, 1).totalElementos())
                .as("um produto que ja existe nao pode ser inserido outra vez")
                .isEqualTo(produtosAntes);

        assertThat(todosOsPontos())
                .as("nem uma secao do mapa")
                .hasSize(pontosAntes);
    }

    @Test
    void naoHaCorredorRepetidoNemPontoDeServicoDuplicado() {
        Map<String, Long> porCorredor = pontoMapaRepository.buscarPorTipo(TipoPonto.PRATELEIRA)
                .stream()
                .collect(Collectors.groupingBy(PontoMapa::getCorredor, Collectors.counting()));

        System.out.println(">>> secoes: " + porCorredor);

        assertThat(porCorredor.values())
                .as("secao repetida dividiria os produtos de um corredor em dois pontos do mapa")
                .allMatch(quantidade -> quantidade == 1);

        for (TipoPonto tipo : List.of(TipoPonto.CAIXA, TipoPonto.BANHEIRO)) {
            assertThat(pontoMapaRepository.buscarPorTipo(tipo))
                    .as("ponto de servico %s duplicado apareceria duas vezes no mapa", tipo)
                    .hasSize(1);
        }
    }

    // ---------------------------------------------------------------- apresentacao

    @Test
    void todoProdutoDaMassaTemDescricao() {
        /*
         * A carga e incremental e nunca reescreve um SKU que ja existe, entao os 29 produtos
         * criados antes destes campos ficariam sem descricao para sempre se o passo de
         * completar apresentacoes nao existisse. Este teste roda contra um banco que ja tinha
         * a massa antiga - e por isso ele prova exatamente esse passo.
         */
        List<Produto> semDescricao = todosOsProdutos().stream()
                .filter(produto -> produto.getDescricao() == null || produto.getDescricao().isBlank())
                .toList();

        assertThat(semDescricao)
                .as("produtos sem descricao: %s",
                        semDescricao.stream().map(Produto::getSku).toList())
                .isEmpty();
    }

    @Test
    void aDescricaoCabeNaColuna() {
        // A coluna aceita 1000 caracteres; um texto maior nao falha em teste, falha na carga.
        assertThat(todosOsProdutos()).allSatisfy(produto ->
                assertThat(produto.getDescricao().length())
                        .as("descricao de %s", produto.getSku())
                        .isLessThanOrEqualTo(1000));
    }

    @Test
    void produtoSemImagemContinuaUtilizavel() {
        /*
         * A coleta de URLs e incremental e feita pelo time (O-18): imagem nula e estado normal,
         * nao defeito. O que nao pode e o produto deixar de responder por causa disso.
         */
        List<Produto> semImagem = todosOsProdutos().stream()
                .filter(produto -> produto.getImagemUrl() == null)
                .toList();

        System.out.printf(">>> produtos sem imagem: %d de %d%n",
                semImagem.size(), todosOsProdutos().size());

        assertThat(semImagem).allSatisfy(produto -> {
            assertThat(produto.getNome()).isNotBlank();
            assertThat(produto.getDescricao()).isNotBlank();
            assertThat(produto.getPontoMapa()).isNotNull();
        });
    }

    @Test
    void oCenarioPlantadoContinuaComEstoqueZerado() {
        // A lixa grao 120 e o unico produto que nasce zerado: e o gatilho da demonstracao.
        assertThat(porSku("SKU-TIN-003").temDisponibilidade())
                .as("se alguem restaurar o estoque e esquecer, a demonstracao nao dispara")
                .isFalse();

        PARES_DE_SUBSTITUICAO.values().forEach(sku ->
                assertThat(porSku(sku).temDisponibilidade())
                        .as("o substituto %s precisa ter estoque", sku)
                        .isTrue());
    }

    private List<Produto> todosOsProdutos() {
        return produtoRepository.buscarPaginado(0, 1000).conteudo();
    }

    private List<PontoMapa> todosOsPontos() {
        return List.of(TipoPonto.PRATELEIRA, TipoPonto.CAIXA, TipoPonto.BANHEIRO)
                .stream()
                .flatMap(tipo -> pontoMapaRepository.buscarPorTipo(tipo).stream())
                .collect(Collectors.collectingAndThen(Collectors.toMap(
                        PontoMapa::getId, Function.identity(), (a, b) -> a), m -> List.copyOf(m.values())));
    }
}
