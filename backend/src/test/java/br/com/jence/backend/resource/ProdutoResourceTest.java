package br.com.jence.backend.resource;

import br.com.jence.backend.dto.CatalogoResponse;
import br.com.jence.backend.dto.FacetaResponse;
import br.com.jence.backend.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.dto.ProdutoResponse;
import br.com.jence.backend.dto.SecaoResponse;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.FacetaDeProdutos;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.SecaoDoCatalogo;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.service.BuscarProdutosUseCase;
import br.com.jence.backend.service.ConsultarProdutoUseCase;
import br.com.jence.backend.service.ListarSecoesUseCase;
import br.com.jence.backend.service.SimularEstoqueUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As rotas do catalogo pelo Jersey, com casos de uso falsos. */
class ProdutoResourceTest {

    /** Anota os argumentos de cada busca; devolve o catalogo combinado ou lanca a falha. */
    static class BuscarFalso extends BuscarProdutosUseCase {
        final List<List<Object>> chamadas = new ArrayList<>();
        CatalogoResponse catalogo;
        Exception falha;

        BuscarFalso() {
            super(null);
        }

        @Override
        public CatalogoResponse executar(String termo, String secao, Boolean apenasDisponiveis,
                                         Map<AtributoProduto, List<String>> atributos,
                                         Integer pagina, Integer tamanho) throws SQLException {
            chamadas.add(Arrays.asList(termo, secao, apenasDisponiveis, atributos, pagina, tamanho));
            if (falha instanceof SQLException e) {
                throw e;
            }
            if (falha instanceof RuntimeException e) {
                throw e;
            }
            return catalogo;
        }
    }

    static class ConsultarFalso extends ConsultarProdutoUseCase {
        ProdutoDetalhadoResponse produto;
        RecursoNaoEncontradoException falha;

        ConsultarFalso() {
            super(null);
        }

        @Override
        public ProdutoDetalhadoResponse executar(UUID produtoId) throws RecursoNaoEncontradoException {
            if (falha != null) {
                throw falha;
            }
            return produto;
        }
    }

    static class SimularFalso extends SimularEstoqueUseCase {
        final List<Integer> saldos = new ArrayList<>();
        ProdutoResponse produto;
        RecursoNaoEncontradoException falha;

        SimularFalso() {
            super(null);
        }

        @Override
        public ProdutoResponse executar(UUID produtoId, int novoSaldo) throws RecursoNaoEncontradoException {
            saldos.add(novoSaldo);
            if (falha != null) {
                throw falha;
            }
            return produto;
        }
    }

    static class SecoesFalso extends ListarSecoesUseCase {
        List<SecaoResponse> secoes = List.of();

        SecoesFalso() {
            super(null);
        }

        @Override
        public List<SecaoResponse> executar() {
            return secoes;
        }
    }

    private final BuscarFalso buscar = new BuscarFalso();
    private final ConsultarFalso consultar = new ConsultarFalso();
    private final SimularFalso simular = new SimularFalso();
    private final SecoesFalso secoes = new SecoesFalso();
    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(new ProdutoResource(buscar, consultar, simular, secoes));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    static CatalogoResponse catalogo(List<ProdutoResponse> produtos, int page, int size,
                                     long totalElements, int totalPages,
                                     List<FacetaResponse> facetas) {
        CatalogoResponse catalogo = new CatalogoResponse();
        catalogo.setContent(produtos);
        catalogo.setPage(page);
        catalogo.setSize(size);
        catalogo.setTotalElements(totalElements);
        catalogo.setTotalPages(totalPages);
        catalogo.setFacetas(facetas);
        return catalogo;
    }

    private ProdutoResponse produto(UUID id) {
        return ProdutoResponse.de(new Produto(id, "SKU-TIN-001",
                "Tinta Acrilica Fosca Branca 18L", new BigDecimal("289.90"), 12,
                new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 50, 50)));
    }

    @Test
    @DisplayName("busca sem parametros repassa nulos para o caso de uso decidir os padroes")
    void buscaSemParametros() throws Exception {
        buscar.catalogo = catalogo(List.of(produto(UUID.randomUUID())), 0, 20, 1L, 1, List.of());

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos");

        assertEquals(200, resposta.status());
        assertEquals("Tintas", resposta.texto("content[0].corredor"));
        assertEquals(Arrays.asList(null, null, null, Map.of(), null, null), buscar.chamadas.get(0));
    }

    @Test
    @DisplayName("a listagem traz o corredor, e nao so o id do ponto")
    void listagemTrazCorredor() throws Exception {
        /*
         * Sem este campo a tela nao tem como dizer onde o produto fica sem cruzar cada item
         * com GET /mapa - e enquanto ela nao cruzava, o catalogo inteiro exibia um texto
         * generico no lugar do corredor. Ver D-71.
         */
        buscar.catalogo = catalogo(List.of(produto(UUID.randomUUID())), 0, 20, 1L, 1, List.of());

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos");

        assertEquals("Tintas", resposta.texto("content[0].corredor"));
        assertNotNull(resposta.json("content[0].pontoMapaId"));
    }

    @Test
    @DisplayName("busca repassa todos os filtros exatamente como recebidos")
    void buscaComParametros() throws Exception {
        buscar.catalogo = catalogo(List.of(), 1, 5, 0L, 0, List.of());

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos?query=tinta&secao=Tintas"
                + "&apenasDisponiveis=true&atributo=MARCA:Tigre&atributo=MARCA:Docol"
                + "&atributo=BITOLA:25%20mm&page=1&size=5");

        assertEquals(200, resposta.status());
        List<Object> chamada = buscar.chamadas.get(0);
        assertEquals("tinta", chamada.get(0));
        assertEquals("Tintas", chamada.get(1));
        assertEquals(Boolean.TRUE, chamada.get(2));
        assertEquals(1, chamada.get(4));
        assertEquals(5, chamada.get(5));

        // Duas marcas viram uma chave com dois valores; a bitola vira outra chave.
        @SuppressWarnings("unchecked")
        Map<AtributoProduto, List<String>> atributos = (Map<AtributoProduto, List<String>>) chamada.get(3);
        assertEquals(List.of("Tigre", "Docol"), atributos.get(AtributoProduto.MARCA));
        assertEquals(List.of("25 mm"), atributos.get(AtributoProduto.BITOLA));
    }

    @Test
    @DisplayName("atributo malformado ou desconhecido e ignorado, e nao vira 400")
    void atributoInvalidoEIgnorado() throws Exception {
        /*
         * Chave desconhecida vem de um link antigo, e o cliente nao tem o que fazer a
         * respeito. Ignorar alarga o resultado; um 400 deixaria a tela em branco por causa de
         * um parametro que ele nem sabe que existe.
         */
        buscar.catalogo = catalogo(List.of(), 0, 20, 0L, 0, List.of());

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos?atributo=NAO_EXISTE:algo"
                + "&atributo=semSeparador&atributo=:semChave&atributo=MARCA:Tigre");

        assertEquals(200, resposta.status());
        assertEquals(Map.of(AtributoProduto.MARCA, List.of("Tigre")), buscar.chamadas.get(0).get(3),
                "so o filtro valido sobrevive");
    }

    @Test
    @DisplayName("page que nao e numero devolve 400, e nao 404")
    void paginaInvalida() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos?page=abc");

        assertEquals(400, resposta.status());
        assertEquals("Parâmetro Inválido", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("page"));
        assertTrue(buscar.chamadas.isEmpty());
    }

    @Test
    @DisplayName("query que nao e UTF-8 valido devolve 400, e nao uma busca com termo corrompido")
    void queryIndecodificavel() throws Exception {
        // %E2 e o "a" com acento circunflexo em latin-1; em UTF-8 nao forma caractere (D-74).
        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos?query=L%E2mpada");

        assertEquals(400, resposta.status());
        assertEquals("/api/v1/produtos", resposta.texto("path"));
        assertTrue(resposta.texto("message").toLowerCase().contains("utf-8"));
        assertTrue(buscar.chamadas.isEmpty());
    }

    @Test
    @DisplayName("acento em UTF-8 chega intacto ao caso de uso")
    void acentoEmUtf8() throws Exception {
        buscar.catalogo = catalogo(List.of(), 0, 20, 0L, 0, List.of());

        assertEquals(200, servidor.get("/api/v1/produtos?query=L%C3%A2mpada&secao=Decora%C3%A7%C3%A3o").status());

        assertEquals("Lâmpada", buscar.chamadas.get(0).get(0));
        assertEquals("Decoração", buscar.chamadas.get(0).get(1));
    }

    @Test
    @DisplayName("apenasDisponiveis aceita as grafias que o Spring aceitava")
    void apenasDisponiveisComOutrasGrafias() throws Exception {
        buscar.catalogo = catalogo(List.of(), 0, 20, 0L, 0, List.of());

        assertEquals(200, servidor.get("/api/v1/produtos?apenasDisponiveis=1").status());
        assertEquals(200, servidor.get("/api/v1/produtos?apenasDisponiveis=off").status());
        assertEquals(400, servidor.get("/api/v1/produtos?apenasDisponiveis=talvez").status());

        assertEquals(Boolean.TRUE, buscar.chamadas.get(0).get(2));
        assertEquals(Boolean.FALSE, buscar.chamadas.get(1).get(2));
    }

    @Test
    @DisplayName("a resposta da busca traz as facetas do recorte atual")
    void respostaTrazFacetas() throws Exception {
        buscar.catalogo = catalogo(List.of(), 0, 20, 0L, 0, List.of(
                FacetaResponse.de(new FacetaDeProdutos(AtributoProduto.MARCA, "Marca", List.of(
                        new FacetaDeProdutos.Valor("Tigre", 2),
                        new FacetaDeProdutos.Valor("Docol", 1))))));

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos");

        assertEquals(200, resposta.status());
        assertEquals("MARCA", resposta.texto("facetas[0].atributo"));
        assertEquals("Marca", resposta.texto("facetas[0].rotulo"));
        assertEquals("Tigre", resposta.texto("facetas[0].valores[0].valor"));
        assertEquals(2, resposta.inteiro("facetas[0].valores[0].quantidade"));
    }

    @Test
    @DisplayName("GET /produtos/secoes nao e confundido com o detalhe de um produto")
    void secoesNaoColideComDetalhe() throws Exception {
        /*
         * As duas rotas convivem sob /produtos: uma literal e outra com variavel. Se o Jersey
         * casasse "secoes" com {produtoId}, a resposta seria 400 por UUID malformado.
         */
        secoes.secoes = List.of(SecaoResponse.de(new SecaoDoCatalogo("Tintas", 5)),
                SecaoResponse.de(new SecaoDoCatalogo("Jardim", 2)));

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos/secoes");

        assertEquals(200, resposta.status());
        assertEquals("Tintas", resposta.texto("[0].nome"));
        assertEquals(5, resposta.inteiro("[0].quantidadeProdutos"));
        assertEquals("Jardim", resposta.texto("[1].nome"));
    }

    @Test
    @DisplayName("resposta da busca usa o formato de pagina do contrato")
    void formatoDaPagina() throws Exception {
        UUID id = UUID.randomUUID();
        buscar.catalogo = catalogo(List.of(produto(id)), 0, 10, 25L, 3, List.of());

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos");

        assertEquals(200, resposta.status());
        assertEquals(id.toString(), resposta.texto("content[0].id"));
        assertEquals("SKU-TIN-001", resposta.texto("content[0].sku"));
        assertEquals(12, resposta.inteiro("content[0].saldoEstoque"));
        assertEquals(0, resposta.inteiro("page"));
        assertEquals(10, resposta.inteiro("size"));
        assertEquals(25, resposta.inteiro("totalElements"));
        assertEquals(3, resposta.inteiro("totalPages"));
    }

    @Test
    @DisplayName("detalhamento traz o ponto de mapa aninhado")
    void detalhamento() throws Exception {
        UUID id = UUID.randomUUID();
        UUID pontoId = UUID.randomUUID();
        consultar.produto = ProdutoDetalhadoResponse.de(
                new Produto(id, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                        new BigDecimal("289.90"), 12,
                        new PontoMapa(pontoId, TipoPonto.PRATELEIRA, "Tintas", 32, 10)));

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos/" + id);

        assertEquals(200, resposta.status());
        assertEquals(id.toString(), resposta.texto("id"));
        assertEquals("Tintas", resposta.texto("pontoMapa.corredor"));
        assertEquals(32, resposta.inteiro("pontoMapa.coordenadaX"));
        assertEquals(10, resposta.inteiro("pontoMapa.coordenadaY"));
        assertEquals("PRATELEIRA", resposta.texto("pontoMapa.tipo"));
        assertEquals(pontoId.toString(), resposta.texto("pontoMapaId"));
    }

    @Test
    @DisplayName("produto inexistente vira 404 com corpo de erro padrao")
    void produtoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        consultar.falha = new RecursoNaoEncontradoException("Produto", id);

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos/" + id);

        assertEquals(404, resposta.status());
        assertEquals(404, resposta.inteiro("status"));
        assertEquals("Recurso Não Encontrado", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("Produto"));
    }

    @Test
    @DisplayName("id malformado vira 400")
    void idMalformado() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/produtos/abc");

        assertEquals(400, resposta.status());
        assertEquals("Parâmetro Inválido", resposta.texto("error"));
    }

    // ---------------------------------------------------------------- simulacao de estoque

    private ServidorDeTeste.Resposta ajustarEstoque(UUID produtoId, String corpo) throws Exception {
        return servidor.json("PATCH", "/api/v1/produtos/" + produtoId + "/estoque", corpo);
    }

    private ProdutoResponse lixa(UUID produtoId, int saldo) {
        return ProdutoResponse.de(new Produto(produtoId, "SKU-TIN-003",
                "Lixa para Parede Grao 120", new BigDecimal("3.50"), saldo,
                new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 50, 50)));
    }

    @Test
    @DisplayName("PATCH estoque zera o saldo e devolve o produto atualizado")
    void zerarEstoque() throws Exception {
        UUID produtoId = UUID.randomUUID();
        simular.produto = lixa(produtoId, 0);

        ServidorDeTeste.Resposta resposta = ajustarEstoque(produtoId, """
                {"saldoEstoque": 0}""");

        assertEquals(200, resposta.status());
        assertEquals("SKU-TIN-003", resposta.texto("sku"));
        assertEquals(0, resposta.inteiro("saldoEstoque"));
        assertEquals(List.of(0), simular.saldos);
    }

    @Test
    @DisplayName("PATCH estoque tambem restaura o saldo")
    void restaurarEstoque() throws Exception {
        UUID produtoId = UUID.randomUUID();
        simular.produto = lixa(produtoId, 25);

        ServidorDeTeste.Resposta resposta = ajustarEstoque(produtoId, """
                {"saldoEstoque": 25}""");

        assertEquals(200, resposta.status());
        assertEquals(25, resposta.inteiro("saldoEstoque"));
        assertEquals(List.of(25), simular.saldos);
    }

    @Test
    @DisplayName("saldo negativo devolve 400 antes de chegar ao caso de uso")
    void saldoNegativo() throws Exception {
        ServidorDeTeste.Resposta resposta = ajustarEstoque(UUID.randomUUID(), """
                {"saldoEstoque": -5}""");

        assertEquals(400, resposta.status());
        assertEquals("saldoEstoque", resposta.texto("validationErrors[0].field"));
        assertTrue(simular.saldos.isEmpty());
    }

    @Test
    @DisplayName("corpo sem saldoEstoque devolve 400")
    void corpoIncompleto() throws Exception {
        ServidorDeTeste.Resposta resposta = ajustarEstoque(UUID.randomUUID(), "{}");

        assertEquals(400, resposta.status());
        assertEquals("saldoEstoque", resposta.texto("validationErrors[0].field"));
    }

    @Test
    @DisplayName("produto inexistente devolve 404 tambem na simulacao")
    void simulacaoDeProdutoInexistente() throws Exception {
        UUID produtoId = UUID.randomUUID();
        simular.falha = new RecursoNaoEncontradoException("Produto", produtoId);

        assertEquals(404, ajustarEstoque(produtoId, """
                {"saldoEstoque": 0}""").status());
    }
}
