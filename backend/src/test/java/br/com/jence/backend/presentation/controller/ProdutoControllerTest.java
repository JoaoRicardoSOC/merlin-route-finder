package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.CatalogoResponse;
import br.com.jence.backend.application.dto.FacetaResponse;
import br.com.jence.backend.application.dto.PontoMapaResponse;
import br.com.jence.backend.application.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.dto.SecaoResponse;
import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.application.usecase.BuscarProdutosUseCase;
import br.com.jence.backend.application.usecase.ConsultarProdutoUseCase;
import br.com.jence.backend.application.usecase.ListarSecoesUseCase;
import br.com.jence.backend.application.usecase.SimularEstoqueUseCase;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProdutoController.class)
class ProdutoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BuscarProdutosUseCase buscarProdutosUseCase;
    @MockitoBean ConsultarProdutoUseCase consultarProdutoUseCase;
    @MockitoBean SimularEstoqueUseCase simularEstoqueUseCase;
    @MockitoBean ListarSecoesUseCase listarSecoesUseCase;

    private ProdutoResponse produto(UUID id) {
        return new ProdutoResponse(id, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", null, null,
                new BigDecimal("289.90"), 12, UUID.randomUUID(), "Tintas");
    }

    @Test
    @DisplayName("busca sem parametros repassa nulos para o caso de uso decidir os padroes")
    void buscaSemParametros() throws Exception {
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogoResponse(List.of(produto(UUID.randomUUID())), 0, 20, 1L, 1, List.of()));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].corredor").value("Tintas"));

        verify(buscarProdutosUseCase).executar(null, null, null, Map.of(), null, null);
    }

    @Test
    @DisplayName("a listagem traz o corredor, e nao so o id do ponto")
    void listagemTrazCorredor() throws Exception {
        /*
         * Sem este campo a tela nao tem como dizer onde o produto fica sem cruzar cada item
         * com GET /mapa - e enquanto ela nao cruzava, o catalogo inteiro exibia um texto
         * generico no lugar do corredor. Ver D-71.
         */
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogoResponse(List.of(produto(UUID.randomUUID())), 0, 20, 1L, 1, List.of()));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].corredor").value("Tintas"))
                .andExpect(jsonPath("$.content[0].pontoMapaId").exists());
    }

    @Test
    @DisplayName("busca repassa todos os filtros exatamente como recebidos")
    void buscaComParametros() throws Exception {
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogoResponse(List.of(), 1, 5, 0L, 0, List.of()));

        mockMvc.perform(get("/api/v1/produtos")
                        .param("query", "tinta").param("secao", "Tintas")
                        .param("apenasDisponiveis", "true")
                        .param("atributo", "MARCA:Tigre")
                        .param("atributo", "MARCA:Docol")
                        .param("atributo", "BITOLA:25 mm")
                        .param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> termo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> secao = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Boolean> disponiveis = ArgumentCaptor.forClass(Boolean.class);
        ArgumentCaptor<Map<AtributoProduto, List<String>>> atributos =
                ArgumentCaptor.forClass(Map.class);
        ArgumentCaptor<Integer> pagina = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> tamanho = ArgumentCaptor.forClass(Integer.class);
        verify(buscarProdutosUseCase).executar(termo.capture(), secao.capture(),
                disponiveis.capture(), atributos.capture(), pagina.capture(), tamanho.capture());

        assertThat(termo.getValue()).isEqualTo("tinta");
        assertThat(secao.getValue()).isEqualTo("Tintas");
        assertThat(disponiveis.getValue()).isTrue();
        assertThat(pagina.getValue()).isEqualTo(1);
        assertThat(tamanho.getValue()).isEqualTo(5);

        // Duas marcas viram uma chave com dois valores; a bitola vira outra chave.
        assertThat(atributos.getValue())
                .containsEntry(AtributoProduto.MARCA, List.of("Tigre", "Docol"))
                .containsEntry(AtributoProduto.BITOLA, List.of("25 mm"));
    }

    @Test
    @DisplayName("atributo malformado ou desconhecido e ignorado, e nao vira 400")
    void atributoInvalidoEIgnorado() throws Exception {
        /*
         * Chave desconhecida vem de um link antigo, e o cliente nao tem o que fazer a
         * respeito. Ignorar alarga o resultado; um 400 deixaria a tela em branco por causa de
         * um parametro que ele nem sabe que existe.
         */
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogoResponse(List.of(), 0, 20, 0L, 0, List.of()));

        mockMvc.perform(get("/api/v1/produtos")
                        .param("atributo", "NAO_EXISTE:algo")
                        .param("atributo", "semSeparador")
                        .param("atributo", ":semChave")
                        .param("atributo", "MARCA:Tigre"))
                .andExpect(status().isOk());

        ArgumentCaptor<Map<AtributoProduto, List<String>>> atributos =
                ArgumentCaptor.forClass(Map.class);
        verify(buscarProdutosUseCase).executar(any(), any(), any(),
                atributos.capture(), any(), any());

        assertThat(atributos.getValue())
                .as("so o filtro valido sobrevive")
                .containsExactly(java.util.Map.entry(AtributoProduto.MARCA, List.of("Tigre")));
    }

    @Test
    @DisplayName("a resposta da busca traz as facetas do recorte atual")
    void respostaTrazFacetas() throws Exception {
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogoResponse(List.of(), 0, 20, 0L, 0, List.of(
                        new FacetaResponse(AtributoProduto.MARCA, "Marca", List.of(
                                new FacetaResponse.ValorResponse("Tigre", 2),
                                new FacetaResponse.ValorResponse("Docol", 1))))));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.facetas[0].atributo").value("MARCA"))
                .andExpect(jsonPath("$.facetas[0].rotulo").value("Marca"))
                .andExpect(jsonPath("$.facetas[0].valores[0].valor").value("Tigre"))
                .andExpect(jsonPath("$.facetas[0].valores[0].quantidade").value(2));
    }

    @Test
    @DisplayName("GET /produtos/secoes nao e confundido com o detalhe de um produto")
    void secoesNaoColideComDetalhe() throws Exception {
        /*
         * As duas rotas convivem sob /produtos: uma literal e outra com variavel. Se o Spring
         * casasse "secoes" com {produtoId}, a resposta seria 400 por UUID malformado.
         */
        when(listarSecoesUseCase.executar())
                .thenReturn(List.of(new SecaoResponse("Tintas", 5), new SecaoResponse("Jardim", 2)));

        mockMvc.perform(get("/api/v1/produtos/secoes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nome").value("Tintas"))
                .andExpect(jsonPath("$[0].quantidadeProdutos").value(5))
                .andExpect(jsonPath("$[1].nome").value("Jardim"));
    }

    @Test
    @DisplayName("resposta da busca usa o formato de pagina do contrato")
    void formatoDaPagina() throws Exception {
        UUID id = UUID.randomUUID();
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any()))
                .thenReturn(new CatalogoResponse(List.of(produto(id)), 0, 10, 25L, 3, List.of()));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(id.toString()))
                .andExpect(jsonPath("$.content[0].sku").value("SKU-TIN-001"))
                .andExpect(jsonPath("$.content[0].saldoEstoque").value(12))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(25))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    @DisplayName("detalhamento traz o ponto de mapa aninhado")
    void detalhamento() throws Exception {
        UUID id = UUID.randomUUID();
        UUID pontoId = UUID.randomUUID();
        when(consultarProdutoUseCase.executar(id)).thenReturn(new ProdutoDetalhadoResponse(
                id, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", null, null, new BigDecimal("289.90"), 12,
                pontoId, new PontoMapaResponse(pontoId, TipoPonto.PRATELEIRA, "Tintas", null, 32, 10), List.of()));

        mockMvc.perform(get("/api/v1/produtos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.pontoMapa.corredor").value("Tintas"))
                .andExpect(jsonPath("$.pontoMapa.coordenadaX").value(32))
                .andExpect(jsonPath("$.pontoMapa.coordenadaY").value(10))
                .andExpect(jsonPath("$.pontoMapa.tipo").value("PRATELEIRA"))
                .andExpect(jsonPath("$.pontoMapaId").value(pontoId.toString()));
    }

    @Test
    @DisplayName("produto inexistente vira 404 com corpo de erro padrao")
    void produtoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarProdutoUseCase.executar(any()))
                .thenThrow(new RecursoNaoEncontradoException("Produto", id));

        mockMvc.perform(get("/api/v1/produtos/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso Não Encontrado"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Produto")));
    }

    @Test
    @DisplayName("id malformado vira 400, confirmando a correcao do card anterior neste controller")
    void idMalformado() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parâmetro Inválido"));
    }

    // ---------------------------------------------------------------- simulacao de estoque

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder ajustarEstoque(
            UUID produtoId, String corpo) {
        return patch("/api/v1/produtos/{p}/estoque", produtoId)
                .contentType(APPLICATION_JSON)
                .content(corpo);
    }

    @Test
    @DisplayName("PATCH estoque zera o saldo e devolve o produto atualizado")
    void zerarEstoque() throws Exception {
        UUID produtoId = UUID.randomUUID();
        when(simularEstoqueUseCase.executar(eq(produtoId), eq(0))).thenReturn(
                new ProdutoResponse(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120", null, null,
                        new BigDecimal("3.50"), 0, UUID.randomUUID(), "Tintas"));

        mockMvc.perform(ajustarEstoque(produtoId, """
                        {"saldoEstoque": 0}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-TIN-003"))
                .andExpect(jsonPath("$.saldoEstoque").value(0));
    }

    @Test
    @DisplayName("PATCH estoque tambem restaura o saldo")
    void restaurarEstoque() throws Exception {
        UUID produtoId = UUID.randomUUID();
        when(simularEstoqueUseCase.executar(eq(produtoId), eq(25))).thenReturn(
                new ProdutoResponse(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120", null, null,
                        new BigDecimal("3.50"), 25, UUID.randomUUID(), "Tintas"));

        mockMvc.perform(ajustarEstoque(produtoId, """
                        {"saldoEstoque": 25}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldoEstoque").value(25));
    }

    @Test
    @DisplayName("saldo negativo devolve 400 antes de chegar ao caso de uso")
    void saldoNegativo() throws Exception {
        mockMvc.perform(ajustarEstoque(UUID.randomUUID(), """
                        {"saldoEstoque": -5}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("saldoEstoque"));

        verify(simularEstoqueUseCase, org.mockito.Mockito.never()).executar(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    @DisplayName("corpo sem saldoEstoque devolve 400")
    void corpoIncompleto() throws Exception {
        mockMvc.perform(ajustarEstoque(UUID.randomUUID(), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("saldoEstoque"));
    }

    @Test
    @DisplayName("produto inexistente devolve 404 tambem na simulacao")
    void simulacaoDeProdutoInexistente() throws Exception {
        UUID produtoId = UUID.randomUUID();
        when(simularEstoqueUseCase.executar(any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenThrow(new RecursoNaoEncontradoException("Produto", produtoId));

        mockMvc.perform(ajustarEstoque(produtoId, """
                        {"saldoEstoque": 0}"""))
                .andExpect(status().isNotFound());
    }
}
