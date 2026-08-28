package br.com.jence.backend;

import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.repository.FiltroDeProdutos;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SecaoDoCatalogo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A busca do catalogo contra o Oracle real, que e onde os riscos desta implementacao moram:
 * parametro opcional em consulta montada, paginacao aplicada pelo dialeto, e a busca tolerante
 * a erro de digitacao convivendo com filtros.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class BuscaComFiltrosIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;

    private static final int TAMANHO = 100;

    private List<Produto> buscar(FiltroDeProdutos filtro) {
        return produtoRepository.buscar(filtro, 0, TAMANHO).conteudo();
    }

    private List<String> nomes(List<Produto> produtos) {
        return produtos.stream().map(Produto::getNome).toList();
    }

    // ---------------------------------------------------------------- navegar

    @Test
    @DisplayName("sem filtro nenhum, navega o catalogo em ordem alfabetica e estavel")
    void semFiltro() {
        // O caso que a consulta montada mais arrisca errar: nenhum predicado e acrescentado,
        // e nenhum parametro e vinculado.
        List<Produto> todos = buscar(FiltroDeProdutos.nenhum());

        assertThat(todos).isNotEmpty();

        /*
         * A ordem alfabetica e verificada pela primeira letra, e nao com isSorted() sobre a
         * String inteira, porque quem ordena e o Oracle - e a colacao dele nao e a do Java.
         * Medido: o banco devolve "Lampada LED Filamento" ANTES de "Lampada LED 12W", ou seja
         * ordena letra antes de digito, ao contrario da comparacao natural de String.
         *
         * Nao e defeito: a ordem que o cliente ve e coerente e estavel. Defeito seria o teste
         * afirmar uma colacao que o sistema nunca prometeu.
         */
        List<Character> iniciais = nomes(todos).stream()
                .map(nome -> Character.toUpperCase(nome.charAt(0)))
                .toList();

        assertThat(iniciais).isSorted();
    }

    @Test
    @DisplayName("a ordem do catalogo e a mesma entre chamadas")
    void ordemEstavel() {
        /*
         * E a propriedade que de fato importa para o cliente: a lista nao pode se rearranjar
         * entre uma consulta e outra. Sem ordenacao explicita, o SQL nao garante isso.
         */
        assertThat(nomes(buscar(FiltroDeProdutos.nenhum())))
                .isEqualTo(nomes(buscar(FiltroDeProdutos.nenhum())));
    }

    @Test
    @DisplayName("a paginacao continua correta com a consulta montada")
    void paginacao() {
        Pagina<Produto> primeira = produtoRepository.buscar(FiltroDeProdutos.nenhum(), 0, 3);
        Pagina<Produto> segunda = produtoRepository.buscar(FiltroDeProdutos.nenhum(), 1, 3);

        assertThat(primeira.conteudo()).hasSize(3);
        assertThat(primeira.totalElementos()).isGreaterThan(3);
        assertThat(primeira.totalPaginas())
                .isEqualTo((int) Math.ceil(primeira.totalElementos() / 3.0));
        assertThat(nomes(segunda.conteudo()))
                .as("a segunda pagina precisa continuar de onde a primeira parou")
                .doesNotContainAnyElementsOf(nomes(primeira.conteudo()));
    }

    // ---------------------------------------------------------------- secao

    @Test
    @DisplayName("filtrar por secao devolve so produtos dela")
    void porSecao() {
        List<Produto> tintas = buscar(new FiltroDeProdutos(null, "Tintas", false));

        assertThat(tintas).isNotEmpty();
        assertThat(tintas).allSatisfy(produto ->
                assertThat(produto.getPontoMapa().getCorredor()).isEqualTo("Tintas"));
    }

    @Test
    @DisplayName("a secao nao depende de maiuscula ou minuscula")
    void secaoSemCaixa() {
        // O nome chega de um menu, mas tambem pode chegar de uma URL colada ou digitada.
        assertThat(buscar(new FiltroDeProdutos(null, "tintas", false)))
                .hasSameSizeAs(buscar(new FiltroDeProdutos(null, "Tintas", false)));
    }

    @Test
    @DisplayName("secao inexistente devolve vazio, e nao o catalogo inteiro")
    void secaoInexistente() {
        // O erro classico de predicado opcional: montar a condicao e nunca aplica-la.
        assertThat(buscar(new FiltroDeProdutos(null, "Secao Que Nao Existe", false))).isEmpty();
    }

    // ---------------------------------------------------------------- disponibilidade

    @Test
    @DisplayName("sem o filtro, o produto zerado aparece")
    void zeradoApareceporPadrao() {
        /*
         * De proposito: o cliente precisa ver que o produto existe na loja para entender que
         * ele esta esgotado. Esconder do catalogo transformaria "acabou" em "nao vendemos".
         */
        assertThat(nomes(buscar(FiltroDeProdutos.nenhum())))
                .contains("Pincel Chato 2 Polegadas");
    }

    @Test
    @DisplayName("com o filtro, os zerados somem")
    void apenasDisponiveis() {
        List<Produto> disponiveis = buscar(new FiltroDeProdutos(null, null, true));

        assertThat(disponiveis).isNotEmpty();
        assertThat(disponiveis).allSatisfy(produto ->
                assertThat(produto.temDisponibilidade()).isTrue());
        assertThat(nomes(disponiveis)).doesNotContain("Pincel Chato 2 Polegadas");
    }

    // ---------------------------------------------------------------- termo, e termo com filtro

    @Test
    @DisplayName("a busca tolerante a erro de digitacao continua funcionando")
    void erroDeDigitacao() {
        // A garantia que a unificacao das consultas mais arriscava perder.
        assertThat(nomes(buscar(FiltroDeProdutos.porTermo("tnta"))))
                .anyMatch(nome -> nome.startsWith("Tinta"));
    }

    @Test
    @DisplayName("termo e secao trabalham juntos")
    void termoComSecao() {
        List<Produto> lixasEmTintas = buscar(new FiltroDeProdutos("lixa", "Tintas", false));

        System.out.println(">>> lixa em Tintas: " + nomes(lixasEmTintas));

        assertThat(lixasEmTintas).isNotEmpty();
        assertThat(lixasEmTintas).allSatisfy(produto -> {
            assertThat(produto.getNome().toLowerCase()).contains("lixa");
            assertThat(produto.getPontoMapa().getCorredor()).isEqualTo("Tintas");
        });
    }

    @Test
    @DisplayName("os tres filtros combinam de uma vez")
    void tresFiltrosJuntos() {
        /*
         * Iluminacao, e nao Tintas, porque o teste precisa de um termo que case com VARIOS
         * produtos da secao sendo que so um esta zerado - senao o filtro de disponibilidade
         * esvazia o resultado e nao ha o que afirmar. "lampada" casa com cinco; a amarela e
         * a unica sem estoque.
         *
         * O termo vai ACENTUADO porque este teste e sobre combinar filtros, e nao sobre
         * tolerancia a acento - essa tem testes proprios. Sem acento, o LIKE nao casa e o
         * Jaro-Winkler da 69 contra um corte de 70, e o resultado vem vazio (D-73).
         */
        List<Produto> resultado = buscar(new FiltroDeProdutos("lâmpada", "Iluminação", true));

        assertThat(resultado).isNotEmpty();
        assertThat(nomes(resultado))
                .as("a lampada amarela esta zerada e o filtro de disponibilidade a exclui")
                .doesNotContain("Lâmpada LED 9W Amarela - kit 3");
    }

    @Test
    @DisplayName("combinacao sem resultado devolve pagina vazia, nao erro")
    void combinacaoSemResultado() {
        /*
         * O cliente filtrou demais; ele nao pediu um recurso que nao existe.
         *
         * O termo mudou de "lixa" para "disjuntor" quando o catalogo cresceu: "lixa" passou a
         * encontrar a "Lixeira de Embutir" de Cozinhas pela similaridade, o que e a busca
         * tolerante funcionando - e nao um resultado errado.
         */
        Pagina<Produto> vazia = produtoRepository.buscar(
                new FiltroDeProdutos("disjuntor", "Cozinhas", false), 0, TAMANHO);

        assertThat(vazia.conteudo()).isEmpty();
        assertThat(vazia.totalElementos()).isZero();
        assertThat(vazia.totalPaginas()).isZero();
    }

    // ---------------------------------------------------------------- secoes

    @Test
    @DisplayName("as secoes vem com a contagem certa de produtos")
    void listarSecoes() {
        List<SecaoDoCatalogo> secoes = produtoRepository.listarSecoes();

        System.out.println(">>> secoes: " + secoes);

        assertThat(secoes).isNotEmpty();
        assertThat(secoes.stream().map(SecaoDoCatalogo::nome).toList()).isSorted();

        for (SecaoDoCatalogo secao : secoes) {
            assertThat(secao.quantidadeProdutos())
                    .as("secao sem produto nao deveria aparecer no menu")
                    .isPositive();
            assertThat(buscar(new FiltroDeProdutos(null, secao.nome(), false)))
                    .as("contagem de %s", secao.nome())
                    .hasSize((int) secao.quantidadeProdutos());
        }
    }

    @Test
    @DisplayName("a soma das secoes cobre o catalogo inteiro")
    void secoesCobremTudo() {
        // Se um produto ficasse fora de toda secao, ele seria inalcancavel pela navegacao.
        long soma = produtoRepository.listarSecoes().stream()
                .mapToLong(SecaoDoCatalogo::quantidadeProdutos)
                .sum();

        assertThat(soma).isEqualTo(
                produtoRepository.buscar(FiltroDeProdutos.nenhum(), 0, 1).totalElementos());
    }
}
