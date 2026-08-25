package br.com.jence.backend;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;
import br.com.jence.backend.domain.repository.FacetaDeProdutos;
import br.com.jence.backend.domain.repository.FiltroDeProdutos;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O filtro por caracteristica e o calculo de facetas contra o Oracle real.
 * <p>
 * E a parte do catalogo que mais se parece com um e-commerce, e tambem a que tem mais como
 * dar errado em silencio: um "ou" onde devia haver "e" devolve resultado demais, e uma faceta
 * calculada sobre o recorte errado faz opcoes sumirem da tela sem motivo aparente.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class AtributosEFacetasIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;

    private static final int TAMANHO = 100;

    private List<String> skus(FiltroDeProdutos filtro) {
        return produtoRepository.buscar(filtro, 0, TAMANHO).conteudo().stream()
                .map(Produto::getSku)
                .sorted()
                .toList();
    }

    private FiltroDeProdutos comAtributos(Map<AtributoProduto, List<String>> atributos) {
        return new FiltroDeProdutos(null, null, false, atributos);
    }

    private Optional<FacetaDeProdutos> faceta(List<FacetaDeProdutos> facetas, AtributoProduto qual) {
        return facetas.stream().filter(f -> f.atributo() == qual).findFirst();
    }

    // ---------------------------------------------------------------- a massa

    @Test
    @DisplayName("todo produto da massa tem ao menos marca e tipo")
    void massaCompleta() {
        /*
         * Sem marca, o filtro mais usado de uma loja de construcao fica furado: o produto
         * simplesmente desaparece quando o cliente escolhe qualquer marca.
         */
        for (Produto produto : produtoRepository.buscar(FiltroDeProdutos.nenhum(), 0, TAMANHO).conteudo()) {
            List<AtributoProduto> chaves = produtoRepository.buscarAtributosDe(produto.getId())
                    .stream().map(ValorDeAtributo::atributo).toList();

            assertThat(chaves)
                    .as("caracteristicas de %s", produto.getSku())
                    .contains(AtributoProduto.MARCA, AtributoProduto.TIPO);
        }
    }

    @Test
    @DisplayName("as caracteristicas vem na ordem de exibicao, nao alfabetica")
    void ordemDeExibicao() {
        // Marca antes de medida: e a ordem em que o cliente pensa sobre o produto.
        Produto cano = produtoRepository.buscarPorSku("SKU-ENC-001").orElseThrow();

        List<AtributoProduto> chaves = produtoRepository.buscarAtributosDe(cano.getId())
                .stream().map(ValorDeAtributo::atributo).toList();

        System.out.println(">>> caracteristicas do cano: "
                + produtoRepository.buscarAtributosDe(cano.getId()));

        assertThat(chaves).isSorted();
        assertThat(chaves.get(0)).isEqualTo(AtributoProduto.MARCA);
    }

    // ---------------------------------------------------------------- filtro por caracteristica

    @Test
    @DisplayName("um valor filtra para os produtos que o tem")
    void umValor() {
        List<String> tigre = skus(comAtributos(Map.of(AtributoProduto.MARCA, List.of("Tigre"))));

        assertThat(tigre).containsExactly("SKU-ENC-001", "SKU-ENC-002");
    }

    @Test
    @DisplayName("dois valores da mesma chave sao OU")
    void doisValoresDaMesmaChave() {
        List<String> tigreOuDocol = skus(comAtributos(
                Map.of(AtributoProduto.MARCA, List.of("Tigre", "Docol"))));

        assertThat(tigreOuDocol)
                .as("marcar duas marcas mostra as duas, e nao a intersecao vazia entre elas")
                .containsExactly("SKU-COZ-002", "SKU-ENC-001", "SKU-ENC-002", "SKU-ENC-003");
    }

    @Test
    @DisplayName("chaves diferentes sao E")
    void chavesDiferentes() {
        /*
         * O erro que um JOIN unico com todos os valores num IN cometeria: devolver tudo que
         * fosse Tigre OU 25 mm, quando o cliente pediu o cano da Tigre de 25 mm.
         */
        List<String> tigreDe25mm = skus(comAtributos(Map.of(
                AtributoProduto.MARCA, List.of("Tigre"),
                AtributoProduto.BITOLA, List.of("25 mm"))));

        assertThat(tigreDe25mm).containsExactly("SKU-ENC-001");
    }

    @Test
    @DisplayName("caracteristica combina com secao, termo e disponibilidade")
    void combinaComOsOutrosFiltros() {
        List<String> lixaNorton = skus(new FiltroDeProdutos("lixa", "Tintas", true,
                Map.of(AtributoProduto.MARCA, List.of("Norton"))));

        assertThat(lixaNorton)
                .as("a lixa grao 120 e Norton, mas esta zerada")
                .containsExactly("SKU-TIN-004");
    }

    @Test
    @DisplayName("combinacao impossivel devolve vazio, nao erro")
    void combinacaoImpossivel() {
        assertThat(skus(comAtributos(Map.of(
                AtributoProduto.MARCA, List.of("Tigre"),
                AtributoProduto.GRAO, List.of("120"))))).isEmpty();
    }

    @Test
    @DisplayName("valor inexistente nao devolve o catalogo inteiro")
    void valorInexistente() {
        // O erro classico de predicado montado: construir a condicao e nunca aplica-la.
        assertThat(skus(comAtributos(
                Map.of(AtributoProduto.MARCA, List.of("Marca Que Nao Existe"))))).isEmpty();
    }

    // ---------------------------------------------------------------- facetas

    @Test
    @DisplayName("as facetas descrevem o recorte, e nao o catalogo inteiro")
    void facetasDoRecorte() {
        List<FacetaDeProdutos> emTintas = produtoRepository.calcularFacetas(
                new FiltroDeProdutos(null, "Tintas", false));

        System.out.println(">>> facetas em Tintas: " + emTintas.stream()
                .map(FacetaDeProdutos::rotulo).toList());

        /*
         * A razao de as facetas serem calculadas sobre o resultado: "Amperagem" nao pode
         * aparecer para quem navega em Tintas. Um filtro que nao se aplica a nada e pior do
         * que filtro nenhum - o cliente o experimenta e nada acontece.
         */
        assertThat(faceta(emTintas, AtributoProduto.GRAO)).isPresent();
        assertThat(faceta(emTintas, AtributoProduto.AMPERAGEM)).isEmpty();

        assertThat(faceta(emTintas, AtributoProduto.GRAO).orElseThrow().valores())
                .extracting(FacetaDeProdutos.Valor::valor)
                .containsExactlyInAnyOrder("120", "150");
    }

    @Test
    @DisplayName("Amperagem aparece em Eletrica, e Grao nao")
    void facetasMudamComASecao() {
        List<FacetaDeProdutos> emEletrica = produtoRepository.calcularFacetas(
                new FiltroDeProdutos(null, "Eletrica", false));

        assertThat(faceta(emEletrica, AtributoProduto.AMPERAGEM)).isPresent();
        assertThat(faceta(emEletrica, AtributoProduto.GRAO)).isEmpty();
    }

    @Test
    @DisplayName("a contagem de cada valor bate com a busca correspondente")
    void contagemConfere() {
        List<FacetaDeProdutos> todas = produtoRepository.calcularFacetas(FiltroDeProdutos.nenhum());

        for (FacetaDeProdutos faceta : todas) {
            for (FacetaDeProdutos.Valor valor : faceta.valores()) {
                assertThat(skus(comAtributos(Map.of(faceta.atributo(), List.of(valor.valor())))))
                        .as("%s = %s", faceta.rotulo(), valor.valor())
                        .hasSize((int) valor.quantidade());
            }
        }
    }

    @Test
    @DisplayName("marca vem primeiro, e o valor mais comum vem antes dentro dela")
    void ordenacaoDasFacetas() {
        List<FacetaDeProdutos> todas = produtoRepository.calcularFacetas(FiltroDeProdutos.nenhum());

        assertThat(todas.get(0).atributo())
                .as("marca e o filtro mais usado numa loja de construcao")
                .isEqualTo(AtributoProduto.MARCA);

        assertThat(todas).allSatisfy(faceta ->
                assertThat(faceta.valores())
                        .extracting(FacetaDeProdutos.Valor::quantidade)
                        .isSortedAccordingTo(java.util.Comparator.reverseOrder()));
    }

    @Test
    @DisplayName("a faceta ignora a propria escolha do cliente, para ele poder trocar de ideia")
    void facetaIgnoraEscolhaDoCliente() {
        /*
         * Se as facetas fossem calculadas sobre o resultado final, escolher Tigre faria as
         * outras marcas sumirem - e para trocar para Docol o cliente teria que limpar o filtro
         * primeiro. E o comportamento que mais irrita num filtro de e-commerce.
         */
        FiltroDeProdutos comTigre = comAtributos(Map.of(AtributoProduto.MARCA, List.of("Tigre")));

        List<String> marcasVisiveis = faceta(
                produtoRepository.calcularFacetas(comTigre.semAtributos()), AtributoProduto.MARCA)
                .orElseThrow().valores().stream()
                .map(FacetaDeProdutos.Valor::valor)
                .toList();

        assertThat(marcasVisiveis).contains("Tigre", "Docol", "Norton");
    }
}
