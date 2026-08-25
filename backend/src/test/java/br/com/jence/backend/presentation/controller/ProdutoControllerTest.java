package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.PaginaResponse;
import br.com.jence.backend.application.dto.PontoMapaResponse;
import br.com.jence.backend.application.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.usecase.BuscarProdutosUseCase;
import br.com.jence.backend.application.usecase.ConsultarProdutoUseCase;
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

    private ProdutoResponse produto(UUID id) {
        return new ProdutoResponse(id, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                new BigDecimal("289.90"), 12, UUID.randomUUID());
    }

    @Test
    @DisplayName("busca sem parametros repassa nulos para o caso de uso decidir os padroes")
    void buscaSemParametros() throws Exception {
        when(buscarProdutosUseCase.executar(any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(produto(UUID.randomUUID())), 0, 20, 1L, 1));

        mockMvc.perform(get("/api/v1/produtos"))
                .andExpect(status().isOk());

        verify(buscarProdutosUseCase).executar(null, null, null);
    }

    @Test
    @DisplayName("busca repassa query, page e size exatamente como recebidos")
    void buscaComParametros() throws Exception {
        when(buscarProdutosUseCase.executar(any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(), 1, 5, 0L, 0));

        mockMvc.perform(get("/api/v1/produtos")
                        .param("query", "tinta").param("page", "1").param("size", "5"))
                .andExpect(status().isOk());

        ArgumentCaptor<String> termo = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Integer> pagina = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> tamanho = ArgumentCaptor.forClass(Integer.class);
        verify(buscarProdutosUseCase).executar(termo.capture(), pagina.capture(), tamanho.capture());

        assertThat(termo.getValue()).isEqualTo("tinta");
        assertThat(pagina.getValue()).isEqualTo(1);
        assertThat(tamanho.getValue()).isEqualTo(5);
    }

    @Test
    @DisplayName("resposta da busca usa o formato de pagina do contrato")
    void formatoDaPagina() throws Exception {
        UUID id = UUID.randomUUID();
        when(buscarProdutosUseCase.executar(any(), any(), any()))
                .thenReturn(new PaginaResponse<>(List.of(produto(id)), 0, 10, 25L, 3));

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
                id, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", new BigDecimal("289.90"), 12,
                pontoId, new PontoMapaResponse(pontoId, TipoPonto.PRATELEIRA, "Tintas", null, 32, 10)));

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
                .andExpect(jsonPath("$.error").value("Recurso Nao Encontrado"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Produto")));
    }

    @Test
    @DisplayName("id malformado vira 400, confirmando a correcao do card anterior neste controller")
    void idMalformado() throws Exception {
        mockMvc.perform(get("/api/v1/produtos/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parametro Invalido"));
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
                new ProdutoResponse(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120",
                        new BigDecimal("3.50"), 0, UUID.randomUUID()));

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
                new ProdutoResponse(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120",
                        new BigDecimal("3.50"), 25, UUID.randomUUID()));

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
