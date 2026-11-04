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
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

            String mensagem = String.format("caracteristicas de %s", produto.getSku());
            assertTrue(chaves.contains(AtributoProduto.MARCA), mensagem);
            assertTrue(chaves.contains(AtributoProduto.TIPO), mensagem);
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

        assertEquals(chaves.stream().sorted().toList(), chaves);
        assertEquals(AtributoProduto.MARCA, chaves.get(0));
    }

    // ---------------------------------------------------------------- filtro por caracteristica

    @Test
    @DisplayName("um valor filtra para os produtos que o tem, e so para eles")
    void umValor() {
        /*
         * A verificacao e da propriedade, e nao de uma lista fixa de SKUs: uma lista fixa
         * quebra a cada produto acrescentado a massa, sem que nada de errado tenha acontecido
         * - foi o que aconteceu quando o catalogo passou de 29 para 111.
         */
        List<String> tigre = skus(comAtributos(Map.of(AtributoProduto.MARCA, List.of("Tigre"))));

        assertFalse(tigre.isEmpty());
        assertTrue(tigre.contains("SKU-ENC-001"));
        for (String sku : tigre) {
            assertTrue(marcasDe(sku).contains("Tigre"));
        }
    }

    @Test
    @DisplayName("dois valores da mesma chave sao OU: o resultado e a uniao dos dois")
    void doisValoresDaMesmaChave() {
        /*
         * Duas marcas que existem de fato no catalogo. Antes eram Tigre e Docol; com os nomes
         * reais da loja (D-85) a Docol saiu da massa, e um teste de UNIAO sobre uma marca sem
         * produto nenhum deixaria de testar a uniao.
         */
        List<String> tigre = skus(comAtributos(Map.of(AtributoProduto.MARCA, List.of("Tigre"))));
        List<String> dexter = skus(comAtributos(Map.of(AtributoProduto.MARCA, List.of("Dexter"))));

        List<String> tigreOuDexter = skus(comAtributos(
                Map.of(AtributoProduto.MARCA, List.of("Tigre", "Dexter"))));

        // Comparacao em qualquer ordem: as duas listas ordenadas tem de ser iguais.
        assertEquals(
                Stream.concat(tigre.stream(), dexter.stream()).distinct().sorted().toList(),
                tigreOuDexter.stream().sorted().toList(),
                "marcar duas marcas mostra as duas, e nao a intersecao vazia entre elas");
        assertTrue(tigreOuDexter.size() > tigre.size());
    }

    @Test
    @DisplayName("chaves diferentes sao E: o resultado e a intersecao")
    void chavesDiferentes() {
        /*
         * O erro que um JOIN unico com todos os valores num IN cometeria: devolver tudo que
         * fosse Tigre OU 25 mm, quando o cliente pediu o cano da Tigre de 25 mm.
         */
        List<String> tigre = skus(comAtributos(Map.of(AtributoProduto.MARCA, List.of("Tigre"))));
        List<String> de25mm = skus(comAtributos(Map.of(AtributoProduto.BITOLA, List.of("25 mm"))));

        List<String> tigreDe25mm = skus(comAtributos(Map.of(
                AtributoProduto.MARCA, List.of("Tigre"),
                AtributoProduto.BITOLA, List.of("25 mm"))));

        assertFalse(tigreDe25mm.isEmpty());
        assertEquals(
                tigre.stream().filter(de25mm::contains).sorted().toList(),
                tigreDe25mm.stream().sorted().toList());
        String mensagem = "a intersecao precisa ser menor que cada filtro isolado, ou o E virou OU";
        assertTrue(tigreDe25mm.size() < tigre.size(), mensagem);
        assertTrue(tigreDe25mm.size() < de25mm.size(), mensagem);
    }

    private List<String> marcasDe(String sku) {
        return produtoRepository.buscarAtributosDe(
                        produtoRepository.buscarPorSku(sku).orElseThrow().getId()).stream()
                .filter(atributo -> atributo.atributo() == AtributoProduto.MARCA)
                .map(ValorDeAtributo::valor)
                .toList();
    }

    @Test
    @DisplayName("caracteristica combina com secao, termo e disponibilidade")
    void combinaComOsOutrosFiltros() {
        /*
         * Era Philips, e a Kian herdou o cenario inteiro quando os nomes reais entraram: a
         * ILU-001 tem estoque e a ILU-004 - a lampada amarela - esta zerada, as duas Kian.
         * O que o teste guarda continua o mesmo: o filtro de disponibilidade tem que cortar a
         * zerada mesmo quando a marca casa.
         */
        List<String> lampadaKian = skus(new FiltroDeProdutos("lâmpada", "Iluminação", true,
                Map.of(AtributoProduto.MARCA, List.of("Kian"))));

        String mensagem = "a lampada amarela tambem e Kian, mas esta zerada";
        assertTrue(lampadaKian.contains("SKU-ILU-001"), mensagem);
        assertFalse(lampadaKian.contains("SKU-ILU-004"), mensagem);
    }

    @Test
    @DisplayName("combinacao impossivel devolve vazio, nao erro")
    void combinacaoImpossivel() {
        assertTrue(skus(comAtributos(Map.of(
                AtributoProduto.MARCA, List.of("Tigre"),
                AtributoProduto.GRAO, List.of("120")))).isEmpty());
    }

    @Test
    @DisplayName("valor inexistente nao devolve o catalogo inteiro")
    void valorInexistente() {
        // O erro classico de predicado montado: construir a condicao e nunca aplica-la.
        assertTrue(skus(comAtributos(
                Map.of(AtributoProduto.MARCA, List.of("Marca Que Nao Existe")))).isEmpty());
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
        assertTrue(faceta(emTintas, AtributoProduto.GRAO).isPresent());
        assertTrue(faceta(emTintas, AtributoProduto.AMPERAGEM).isEmpty());

        assertEquals(List.of("120", "150"),
                faceta(emTintas, AtributoProduto.GRAO).orElseThrow().valores().stream()
                        .map(FacetaDeProdutos.Valor::valor)
                        .sorted()
                        .toList());
    }

    @Test
    @DisplayName("Amperagem aparece em Elétrica, e Grão não")
    void facetasMudamComASecao() {
        List<FacetaDeProdutos> emEletrica = produtoRepository.calcularFacetas(
                new FiltroDeProdutos(null, "Elétrica", false));

        assertTrue(faceta(emEletrica, AtributoProduto.AMPERAGEM).isPresent());
        assertTrue(faceta(emEletrica, AtributoProduto.GRAO).isEmpty());
    }

    @Test
    @DisplayName("a contagem de cada valor bate com a busca correspondente")
    void contagemConfere() {
        List<FacetaDeProdutos> todas = produtoRepository.calcularFacetas(FiltroDeProdutos.nenhum());

        for (FacetaDeProdutos faceta : todas) {
            for (FacetaDeProdutos.Valor valor : faceta.valores()) {
                assertEquals((int) valor.quantidade(),
                        skus(comAtributos(Map.of(faceta.atributo(), List.of(valor.valor())))).size(),
                        String.format("%s = %s", faceta.rotulo(), valor.valor()));
            }
        }
    }

    @Test
    @DisplayName("marca vem primeiro, e o valor mais comum vem antes dentro dela")
    void ordenacaoDasFacetas() {
        List<FacetaDeProdutos> todas = produtoRepository.calcularFacetas(FiltroDeProdutos.nenhum());

        assertEquals(AtributoProduto.MARCA, todas.get(0).atributo(),
                "marca e o filtro mais usado numa loja de construcao");

        for (FacetaDeProdutos faceta : todas) {
            List<Long> quantidades = faceta.valores().stream()
                    .map(FacetaDeProdutos.Valor::quantidade)
                    .toList();
            assertEquals(quantidades.stream().sorted(java.util.Comparator.reverseOrder()).toList(),
                    quantidades);
        }
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

        assertTrue(marcasVisiveis.contains("Tigre"));
        assertTrue(marcasVisiveis.contains("Dexter"));
        assertTrue(marcasVisiveis.contains("Standers"));
    }
}
