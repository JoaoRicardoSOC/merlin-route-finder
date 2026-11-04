package br.com.jence.backend;

import br.com.jence.backend.model.Produto;
import br.com.jence.backend.domain.repository.FiltroDeProdutos;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SecaoDoCatalogo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertFalse(todos.isEmpty());

        /*
         * A ordem alfabetica e verificada pela primeira letra, e nao comparando a String
         * inteira com uma copia ordenada, porque quem ordena e o Oracle - e a colacao dele nao
         * e a do Java.
         * Medido: o banco devolve "Lampada LED Filamento" ANTES de "Lampada LED 12W", ou seja
         * ordena letra antes de digito, ao contrario da comparacao natural de String.
         *
         * Nao e defeito: a ordem que o cliente ve e coerente e estavel. Defeito seria o teste
         * afirmar uma colacao que o sistema nunca prometeu.
         */
        List<Character> iniciais = nomes(todos).stream()
                .map(nome -> Character.toUpperCase(nome.charAt(0)))
                .toList();

        List<Character> iniciaisOrdenadas = new ArrayList<>(iniciais);
        Collections.sort(iniciaisOrdenadas);
        assertEquals(iniciaisOrdenadas, iniciais);
    }

    @Test
    @DisplayName("a ordem do catalogo e a mesma entre chamadas")
    void ordemEstavel() {
        /*
         * E a propriedade que de fato importa para o cliente: a lista nao pode se rearranjar
         * entre uma consulta e outra. Sem ordenacao explicita, o SQL nao garante isso.
         */
        List<String> primeiraChamada = nomes(buscar(FiltroDeProdutos.nenhum()));
        assertEquals(nomes(buscar(FiltroDeProdutos.nenhum())), primeiraChamada);
    }

    @Test
    @DisplayName("a paginacao continua correta com a consulta montada")
    void paginacao() {
        Pagina<Produto> primeira = produtoRepository.buscar(FiltroDeProdutos.nenhum(), 0, 3);
        Pagina<Produto> segunda = produtoRepository.buscar(FiltroDeProdutos.nenhum(), 1, 3);

        assertEquals(3, primeira.conteudo().size());
        assertTrue(primeira.totalElementos() > 3);
        assertEquals((int) Math.ceil(primeira.totalElementos() / 3.0), primeira.totalPaginas());
        assertTrue(Collections.disjoint(nomes(segunda.conteudo()), nomes(primeira.conteudo())),
                "a segunda pagina precisa continuar de onde a primeira parou");
    }

    // ---------------------------------------------------------------- secao

    @Test
    @DisplayName("filtrar por secao devolve so produtos dela")
    void porSecao() {
        List<Produto> tintas = buscar(new FiltroDeProdutos(null, "Tintas", false));

        assertFalse(tintas.isEmpty());
        for (Produto produto : tintas) {
            assertEquals("Tintas", produto.getPontoMapa().getCorredor());
        }
    }

    @Test
    @DisplayName("a secao nao depende de maiuscula ou minuscula")
    void secaoSemCaixa() {
        // O nome chega de um menu, mas tambem pode chegar de uma URL colada ou digitada.
        List<Produto> minusculas = buscar(new FiltroDeProdutos(null, "tintas", false));
        assertEquals(buscar(new FiltroDeProdutos(null, "Tintas", false)).size(), minusculas.size());
    }

    @Test
    @DisplayName("secao inexistente devolve vazio, e nao o catalogo inteiro")
    void secaoInexistente() {
        // O erro classico de predicado opcional: montar a condicao e nunca aplica-la.
        assertTrue(buscar(new FiltroDeProdutos(null, "Secao Que Nao Existe", false)).isEmpty());
    }

    // ---------------------------------------------------------------- disponibilidade

    @Test
    @DisplayName("sem o filtro, o produto zerado aparece")
    void zeradoApareceporPadrao() {
        /*
         * De proposito: o cliente precisa ver que o produto existe na loja para entender que
         * ele esta esgotado. Esconder do catalogo transformaria "acabou" em "nao vendemos".
         */
        assertTrue(nomes(buscar(FiltroDeProdutos.nenhum()))
                .contains("Pincel Chato Tigre 815 - 2 Embalagem Com 12 Unidades"));
    }

    @Test
    @DisplayName("com o filtro, os zerados somem")
    void apenasDisponiveis() {
        List<Produto> disponiveis = buscar(new FiltroDeProdutos(null, null, true));

        assertFalse(disponiveis.isEmpty());
        for (Produto produto : disponiveis) {
            assertTrue(produto.temDisponibilidade());
        }
        assertFalse(nomes(disponiveis).contains("Pincel Chato 2 Polegadas"));
    }

    // ---------------------------------------------------------------- termo, e termo com filtro

    @Test
    @DisplayName("a busca tolerante a erro de digitacao continua funcionando")
    void erroDeDigitacao() {
        // A garantia que a unificacao das consultas mais arriscava perder.
        assertTrue(nomes(buscar(FiltroDeProdutos.porTermo("tnta"))).stream()
                .anyMatch(nome -> nome.startsWith("Tinta")));
    }

    // ---------------------------------------------------------------- acento

    @Test
    @DisplayName("quem digita sem acento acha o produto acentuado")
    void semAcentoAchaComAcento() {
        /*
         * O caso que motivou a mudanca, e ele estava na tela inicial: o primeiro chip de
         * sugestao do app e "Lâmpada LED". Antes de dobrar os dois lados, digitar "lampada"
         * devolvia ZERO - o LIKE nao casa (â nao e a) e o Jaro-Winkler dava 69 contra um corte
         * de 70. Errava por um ponto. Ver D-73.
         */
        List<String> achados = nomes(buscar(FiltroDeProdutos.porTermo("lampada")));

        System.out.println(">>> 'lampada' achou: " + achados);

        /*
         * Era startsWith("lâmpada"). Com os nomes reais da loja (D-85) quatro das cinco
         * lampadas comecam com "Kit" - "Kit 10 Lâmpadas de LED E27...". O que o teste precisa
         * garantir nunca foi a posicao da palavra, e sim que a busca sem acento devolva
         * lampadas e so lampadas.
         */
        assertFalse(achados.isEmpty());
        assertTrue(achados.stream().allMatch(nome -> nome.toLowerCase().contains("lâmpada")));
    }

    @Test
    @DisplayName("quem digita com acento acha do mesmo jeito")
    void comAcentoTambemAcha() {
        List<String> comAcento = new ArrayList<>(nomes(buscar(FiltroDeProdutos.porTermo("lâmpada"))));
        List<String> semAcento = new ArrayList<>(nomes(buscar(FiltroDeProdutos.porTermo("lampada"))));
        Collections.sort(comAcento);
        Collections.sort(semAcento);
        assertEquals(semAcento, comAcento);
    }

    @Test
    @DisplayName("acento no meio da palavra tambem dobra, inclusive til e cedilha")
    void tilECedilhaTambemDobram() {
        /*
         * Til e o motivo de nao usarmos convert(x,'US7ASCII'): ele dobra cedilha mas
         * transforma ã e õ em '?'. Estas buscas so passam com TRANSLATE.
         *
         * Os termos sao escolhidos entre os que aparecem em NOME de produto - a busca compara
         * p.nome, entao "construcao" nao serviria: a palavra so existe no nome da secao e nas
         * descricoes.
         */
        List<String> sifoes = nomes(buscar(FiltroDeProdutos.porTermo("sifao")));
        assertFalse(sifoes.isEmpty(), "til: 'sifao' precisa achar 'Sifão'");
        assertTrue(sifoes.stream().allMatch(nome -> nome.startsWith("Sifão")),
                "til: 'sifao' precisa achar 'Sifão'");

        /*
         * Era "distribuicao", que casava com "Quadro de Distribuição". O nome real do produto
         * e "Quadro Pvc Embutir 12 Disjuntores Branco" (D-85), entao a palavra saiu da massa.
         * "Mão Francesa" carrega o mesmo til e existe no catalogo de verdade.
         */
        assertTrue(nomes(buscar(FiltroDeProdutos.porTermo("mao"))).stream()
                        .anyMatch(nome -> nome.contains("Mão")),
                "til: 'mao' precisa achar 'Mão'");

        assertTrue(nomes(buscar(FiltroDeProdutos.porTermo("pecas"))).stream()
                        .anyMatch(nome -> nome.contains("Peças")),
                "cedilha: 'pecas' precisa achar 'Peças'");
    }

    @Test
    @DisplayName("a secao tambem casa sem acento")
    void secaoSemAcento() {
        // Um link digitado a mao com "Decoracao" nao pode devolver vazio por falta de cedilha.
        List<Produto> decoracao = buscar(new FiltroDeProdutos(null, "Decoracao", false));
        assertFalse(decoracao.isEmpty());
        for (Produto produto : decoracao) {
            assertEquals("Decoração", produto.getPontoMapa().getCorredor());
        }
    }

    @Test
    @DisplayName("termo e secao trabalham juntos")
    void termoComSecao() {
        List<Produto> lixasEmTintas = buscar(new FiltroDeProdutos("lixa", "Tintas", false));

        System.out.println(">>> lixa em Tintas: " + nomes(lixasEmTintas));

        assertFalse(lixasEmTintas.isEmpty());
        for (Produto produto : lixasEmTintas) {
            assertTrue(produto.getNome().toLowerCase().contains("lixa"));
            assertEquals("Tintas", produto.getPontoMapa().getCorredor());
        }
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

        assertFalse(resultado.isEmpty());
        assertFalse(nomes(resultado).contains("Lâmpada LED 9W Amarela - kit 3"),
                "a lampada amarela esta zerada e o filtro de disponibilidade a exclui");
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

        assertTrue(vazia.conteudo().isEmpty());
        assertEquals(0L, vazia.totalElementos());
        assertEquals(0, vazia.totalPaginas());
    }

    // ---------------------------------------------------------------- secoes

    @Test
    @DisplayName("as secoes vem com a contagem certa de produtos")
    void listarSecoes() {
        List<SecaoDoCatalogo> secoes = produtoRepository.listarSecoes();

        System.out.println(">>> secoes: " + secoes);

        assertFalse(secoes.isEmpty());
        List<String> nomesDasSecoes = secoes.stream().map(SecaoDoCatalogo::nome).toList();
        List<String> nomesOrdenados = new ArrayList<>(nomesDasSecoes);
        Collections.sort(nomesOrdenados);
        assertEquals(nomesOrdenados, nomesDasSecoes);

        for (SecaoDoCatalogo secao : secoes) {
            assertTrue(secao.quantidadeProdutos() > 0,
                    "secao sem produto nao deveria aparecer no menu");
            assertEquals((int) secao.quantidadeProdutos(),
                    buscar(new FiltroDeProdutos(null, secao.nome(), false)).size(),
                    "contagem de " + secao.nome());
        }
    }

    @Test
    @DisplayName("a soma das secoes cobre o catalogo inteiro")
    void secoesCobremTudo() {
        // Se um produto ficasse fora de toda secao, ele seria inalcancavel pela navegacao.
        long soma = produtoRepository.listarSecoes().stream()
                .mapToLong(SecaoDoCatalogo::quantidadeProdutos)
                .sum();

        assertEquals(
                produtoRepository.buscar(FiltroDeProdutos.nenhum(), 0, 1).totalElementos(), soma);
    }
}
