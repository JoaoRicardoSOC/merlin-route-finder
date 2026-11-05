package br.com.jence.backend.resource;

import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.dto.ListaRoteiroResponse;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.ConsultarListaRoteiroUseCase;
import br.com.jence.backend.service.RemoverProdutoDoRoteiroUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As rotas do roteiro no Totem pelo Jersey, com casos de uso falsos. */
class RoteiroResourceTest {

    static class ConsultarFalso extends ConsultarListaRoteiroUseCase {
        ListaRoteiroResponse lista;

        ConsultarFalso() {
            super(null);
        }

        @Override
        public ListaRoteiroResponse executar(UUID sessaoId) {
            return lista;
        }
    }

    /** Anota cada chamada; devolve o item combinado ou lanca a falha combinada. */
    static class AdicionarFalso extends AdicionarProdutoAoRoteiroUseCase {
        final List<UUID[]> chamadas = new ArrayList<>();
        ItemRoteiroDetalhadoResponse item;
        Exception falha;

        AdicionarFalso() {
            super(null, null, null);
        }

        @Override
        public ItemRoteiroDetalhadoResponse executar(UUID sessaoId, UUID produtoId)
                throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
            chamadas.add(new UUID[] {sessaoId, produtoId});
            if (falha instanceof OperacaoNaoPermitidaException e) {
                throw e;
            }
            return item;
        }
    }

    static class RemoverFalso extends RemoverProdutoDoRoteiroUseCase {
        final List<UUID[]> chamadas = new ArrayList<>();
        RecursoNaoEncontradoException falha;

        RemoverFalso() {
            super(null, null);
        }

        @Override
        public void executar(UUID sessaoId, UUID itemId) throws RecursoNaoEncontradoException {
            chamadas.add(new UUID[] {sessaoId, itemId});
            if (falha != null) {
                throw falha;
            }
        }
    }

    private final UUID sessaoId = UUID.randomUUID();
    private final ConsultarFalso consultar = new ConsultarFalso();
    private final AdicionarFalso adicionar = new AdicionarFalso();
    private final RemoverFalso remover = new RemoverFalso();
    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(new RoteiroResource(consultar, adicionar, remover));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    private ItemRoteiroDetalhadoResponse item(UUID itemId, UUID produtoId) {
        return ItemRoteiroDetalhadoResponse.de(new ItemRoteiro(itemId,
                new Produto(produtoId, "SKU-TIN-001",
                        "Tinta Acrilica Fosca Branca 18L", new BigDecimal("289.90"), 12,
                        new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 50, 50))));
    }

    private ListaRoteiroResponse lista(UUID sessaoId, ItemRoteiroDetalhadoResponse... itens) {
        ListaRoteiroResponse lista = new ListaRoteiroResponse();
        lista.setId(UUID.randomUUID());
        lista.setSessaoId(sessaoId);
        lista.setQuantidadeItens(itens.length);
        lista.setItens(List.of(itens));
        return lista;
    }

    private String itens() {
        return "/api/v1/sessoes/" + sessaoId + "/roteiro/itens";
    }

    @Test
    @DisplayName("GET do roteiro devolve a lista no formato do contrato")
    void consultarRoteiro() throws Exception {
        UUID itemId = UUID.randomUUID();
        consultar.lista = lista(sessaoId, item(itemId, UUID.randomUUID()));

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/sessoes/" + sessaoId + "/roteiro");

        assertEquals(200, resposta.status());
        assertEquals(sessaoId.toString(), resposta.texto("sessaoId"));
        assertEquals(1, resposta.inteiro("quantidadeItens"));
        assertEquals(itemId.toString(), resposta.texto("itens[0].id"));
        assertEquals(false, resposta.json("itens[0].coletado"));
        assertEquals("Tinta Acrilica Fosca Branca 18L", resposta.texto("itens[0].produto.nome"));
    }

    @Test
    @DisplayName("POST adiciona produto e devolve 201 com o item")
    void adicionarProduto() throws Exception {
        UUID produtoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        adicionar.item = item(itemId, produtoId);

        ServidorDeTeste.Resposta resposta =
                servidor.json("POST", itens(), "{\"produtoId\":\"" + produtoId + "\"}");

        assertEquals(201, resposta.status());
        assertEquals(itemId.toString(), resposta.texto("id"));
        assertEquals(produtoId.toString(), resposta.texto("produtoId"));
        assertEquals("SKU-TIN-001", resposta.texto("produto.sku"));
        assertEquals(sessaoId, adicionar.chamadas.get(0)[0]);
        assertEquals(produtoId, adicionar.chamadas.get(0)[1]);
    }

    @Test
    @DisplayName("POST sem produtoId devolve 400 apontando o campo invalido")
    void adicionarSemProdutoId() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.json("POST", itens(), "{}");

        assertEquals(400, resposta.status());
        assertEquals(400, resposta.inteiro("status"));
        assertEquals("Falha de Validação", resposta.texto("error"));
        assertEquals("produtoId", resposta.texto("validationErrors[0].field"));
        assertTrue(resposta.texto("validationErrors[0].message").contains("obrigatorio"));
        assertTrue(adicionar.chamadas.isEmpty());
    }

    @Test
    @DisplayName("POST com JSON malformado devolve 400, nao 500")
    void adicionarComJsonInvalido() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.json("POST", itens(), "{ isso nao e json }");

        assertEquals(400, resposta.status());
        assertEquals("Corpo da Requisição Inválido", resposta.texto("error"));
        assertTrue(adicionar.chamadas.isEmpty());
    }

    @Test
    @DisplayName("POST com corpo que nao e JSON devolve 415, e o caso de uso nem e chamado")
    void adicionarComOutroTipoDeConteudo() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.enviar("POST", itens(), "application/xml", "<xml/>");

        assertEquals(415, resposta.status());
        assertTrue(adicionar.chamadas.isEmpty());
    }

    @Test
    @DisplayName("POST sem corpo devolve 400, nao 500")
    void adicionarSemCorpo() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.json("POST", itens(), "");

        assertEquals(400, resposta.status());
        assertEquals("Corpo da Requisição Inválido", resposta.texto("error"));
    }

    @Test
    @DisplayName("sessao inativa devolve 409, nao 404")
    void sessaoInativa() throws Exception {
        adicionar.falha = new OperacaoNaoPermitidaException(
                "Sessao %s nao esta mais ativa (status COMPLETED)".formatted(sessaoId));

        ServidorDeTeste.Resposta resposta =
                servidor.json("POST", itens(), "{\"produtoId\":\"" + UUID.randomUUID() + "\"}");

        assertEquals(409, resposta.status());
        assertEquals(409, resposta.inteiro("status"));
        assertEquals("Operação Não Permitida", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("nao esta mais ativa"));
    }

    @Test
    @DisplayName("DELETE devolve 204 sem corpo")
    void removerItem() throws Exception {
        UUID itemId = UUID.randomUUID();

        ServidorDeTeste.Resposta resposta = servidor.delete(itens() + "/" + itemId);

        assertEquals(204, resposta.status());
        assertEquals("", resposta.corpo());
        assertEquals(sessaoId, remover.chamadas.get(0)[0]);
        assertEquals(itemId, remover.chamadas.get(0)[1]);
    }

    @Test
    @DisplayName("DELETE de item inexistente devolve 404")
    void removerItemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        remover.falha = new RecursoNaoEncontradoException("Item do roteiro", itemId);

        ServidorDeTeste.Resposta resposta = servidor.delete(itens() + "/" + itemId);

        assertEquals(404, resposta.status());
        assertEquals("Recurso Não Encontrado", resposta.texto("error"));
    }

    @Test
    @DisplayName("UUID malformado no caminho devolve 400, e nao 404")
    void uuidMalformado() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/sessoes/nao-e-uuid/roteiro");

        assertEquals(400, resposta.status());
        assertEquals("Parâmetro Inválido", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("sessaoId"));
    }
}
