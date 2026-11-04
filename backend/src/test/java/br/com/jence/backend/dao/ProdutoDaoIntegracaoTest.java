package br.com.jence.backend.dao;

import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.AfinidadeDeProduto;
import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.FacetaDeProdutos;
import br.com.jence.backend.model.FiltroDeProdutos;
import br.com.jence.backend.model.Pagina;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.SecaoDoCatalogo;
import br.com.jence.backend.model.ValorDeAtributo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ProdutoDao} contra o Oracle, numa transacao que nunca e confirmada.
 * <p>
 * Usa a massa de demonstracao que a carga de dados mantem no banco. As regras de busca em si -
 * acento, erro de digitacao, combinacao de filtros - ja tem testes proprios de ponta a ponta;
 * aqui o foco e o SQL do DAO: parametros na ordem certa, paginacao, leitura das colunas.
 */
@Tag("integracao")
class ProdutoDaoIntegracaoTest {

    private final ProdutoDao dao = new ProdutoDao();
    private Transacao transacao;

    @BeforeEach
    void abrir() throws Exception {
        transacao = Transacao.iniciar();
    }

    @AfterEach
    void desfazer() throws Exception {
        transacao.close();
    }

    @Test
    @DisplayName("busca por id e por sku leem o produto com o ponto do mapa")
    void buscaPorIdESku() throws Exception {
        Produto peloSku = dao.buscarPorSku("SKU-FER-002").orElseThrow();
        Produto peloId = dao.buscarPorId(peloSku.getId()).orElseThrow();

        assertEquals(peloSku.getNome(), peloId.getNome());
        assertEquals(peloSku.getPreco(), peloId.getPreco());
        assertEquals(peloSku.getPontoMapa().getId(), peloId.getPontoMapa().getId());
        assertTrue(dao.buscarPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("sem filtro, navega em ordem alfabetica e pagina com offset")
    void paginacao() throws Exception {
        Pagina<Produto> primeira = dao.buscarPaginado(0, 5);
        Pagina<Produto> segunda = dao.buscarPaginado(1, 5);

        assertEquals(5, primeira.conteudo().size());
        assertEquals(primeira.totalElementos(), segunda.totalElementos());
        assertEquals((int) Math.ceil(primeira.totalElementos() / 5.0), primeira.totalPaginas());
        assertTrue(primeira.conteudo().getLast().getNome()
                .compareTo(segunda.conteudo().getFirst().getNome()) <= 0);
        assertNotEquals(primeira.conteudo().getFirst().getId(), segunda.conteudo().getFirst().getId());
    }

    @Test
    @DisplayName("termo sem acento acha produto com acento, e o total bate com a pagina")
    void termoSemAcento() throws Exception {
        Pagina<Produto> pagina = dao.buscarPorTermo("lampada", 0, 50);

        assertFalse(pagina.conteudo().isEmpty());
        assertEquals(pagina.conteudo().size(), pagina.totalElementos());
    }

    @Test
    @DisplayName("filtros combinados: secao, disponibilidade e duas caracteristicas")
    void filtrosCombinados() throws Exception {
        SecaoDoCatalogo secao = dao.listarSecoes().getFirst();
        FiltroDeProdutos soSecao = new FiltroDeProdutos(null, secao.nome(), false);

        Pagina<Produto> daSecao = dao.buscar(soSecao, 0, 100);
        assertEquals(secao.quantidadeProdutos(), daSecao.totalElementos());
        for (Produto produto : daSecao.conteudo()) {
            assertEquals(secao.nome(), produto.getPontoMapa().getCorredor());
        }

        List<FacetaDeProdutos> facetas = dao.calcularFacetas(soSecao);
        assertFalse(facetas.isEmpty());

        FacetaDeProdutos faceta = facetas.getFirst();
        List<String> valores = faceta.valores().stream().map(FacetaDeProdutos.Valor::valor).limit(2).toList();
        long esperado = faceta.valores().stream().limit(2).mapToLong(FacetaDeProdutos.Valor::quantidade).sum();

        FiltroDeProdutos comCaracteristica = new FiltroDeProdutos(
                null, secao.nome(), false, Map.of(faceta.atributo(), valores));
        assertEquals(esperado, dao.buscar(comCaracteristica, 0, 100).totalElementos());

        FiltroDeProdutos disponiveis = new FiltroDeProdutos(null, secao.nome(), true);
        for (Produto produto : dao.buscar(disponiveis, 0, 100).conteudo()) {
            assertTrue(produto.getSaldoEstoque() > 0);
        }
    }

    @Test
    @DisplayName("caracteristicas sao substituidas e voltam na ordem do enum")
    void atributos() throws Exception {
        UUID produto = dao.buscarPorSku("SKU-FER-002").orElseThrow().getId();

        dao.salvarAtributos(produto, List.of(
                new ValorDeAtributo(AtributoProduto.COR, "Azul"),
                new ValorDeAtributo(AtributoProduto.MARCA, "Teste")));

        assertEquals(List.of(
                        new ValorDeAtributo(AtributoProduto.MARCA, "Teste"),
                        new ValorDeAtributo(AtributoProduto.COR, "Azul")),
                dao.buscarAtributosDe(produto));
    }

    @Test
    @DisplayName("caracteristicas de todo o catalogo batem com as de cada produto")
    void atributosDeTodos() throws Exception {
        Map<UUID, List<ValorDeAtributo>> todos = dao.buscarAtributosDeTodos();

        for (Produto produto : dao.buscarPaginado(0, 500).conteudo()) {
            List<ValorDeAtributo> individuais = dao.buscarAtributosDe(produto.getId());
            assertEquals(new java.util.HashSet<>(individuais),
                    new java.util.HashSet<>(todos.getOrDefault(produto.getId(), List.of())),
                    produto.getSku());
        }
    }

    @Test
    @DisplayName("proximos: so disponiveis, dentro do raio, sem o proprio, com limite")
    void proximos() throws Exception {
        Produto referencia = dao.buscarPorSku("SKU-FER-002").orElseThrow();

        List<Produto> proximos = dao.buscarDisponiveisProximosDe(
                referencia.getPontoMapa(), referencia.getId(), AfinidadeDeProduto.nenhuma(), 25, 3);

        assertFalse(proximos.isEmpty());
        assertTrue(proximos.size() <= 3);
        for (Produto produto : proximos) {
            assertNotEquals(referencia.getId(), produto.getId());
            assertTrue(produto.getSaldoEstoque() > 0);
            assertTrue(referencia.getPontoMapa().calcularDistanciaPara(produto.getPontoMapa()) <= 25);
        }
    }

    @Test
    @DisplayName("salvar atualiza o produto existente")
    void salvarAtualiza() throws Exception {
        Produto original = dao.buscarPorSku("SKU-FER-002").orElseThrow();

        dao.salvar(new Produto(original.getId(), original.getSku(), original.getNome(),
                original.getDescricao(), original.getImagemUrl(), new BigDecimal("1.23"), 777,
                original.getPontoMapa()));

        Produto alterado = dao.buscarPorId(original.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("1.23").compareTo(alterado.getPreco()));
        assertEquals(777, alterado.getSaldoEstoque());
        // Nome, descricao e imagem sao tres textos vizinhos no SQL: trocar dois deles nao daria
        // erro nenhum, e o catalogo mostraria a descricao no lugar do nome.
        assertEquals(original.getNome(), alterado.getNome());
        assertEquals(original.getDescricao(), alterado.getDescricao());
        assertEquals(original.getImagemUrl(), alterado.getImagemUrl());
        assertEquals(original.getSku(), alterado.getSku());
    }
}
