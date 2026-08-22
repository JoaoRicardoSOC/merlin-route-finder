package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.dto.ListaRoteiroResponse;
import br.com.jence.backend.application.dto.PontoMapaResponse;
import br.com.jence.backend.application.dto.PontoRotaResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.dto.RotaCalculadaResponse;
import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConsultarListaRoteiroUseCase;
import br.com.jence.backend.application.usecase.IncluirPontoDeInteresseUseCase;
import br.com.jence.backend.application.usecase.RemoverProdutoDoRoteiroUseCase;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoteiroController.class)
class RoteiroControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ConsultarListaRoteiroUseCase consultarListaRoteiroUseCase;
    @MockitoBean AdicionarProdutoAoRoteiroUseCase adicionarProdutoAoRoteiroUseCase;
    @MockitoBean RemoverProdutoDoRoteiroUseCase removerProdutoDoRoteiroUseCase;
    @MockitoBean IncluirPontoDeInteresseUseCase incluirPontoDeInteresseUseCase;

    private final UUID sessaoId = UUID.randomUUID();

    private ItemRoteiroDetalhadoResponse item(UUID itemId, UUID produtoId) {
        return new ItemRoteiroDetalhadoResponse(itemId, produtoId, null, false,
                new ProdutoResponse(produtoId, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                        new BigDecimal("289.90"), 12, UUID.randomUUID()));
    }

    @Test
    @DisplayName("GET do roteiro devolve a lista no formato do contrato")
    void consultarRoteiro() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        when(consultarListaRoteiroUseCase.executar(sessaoId)).thenReturn(
                new ListaRoteiroResponse(UUID.randomUUID(), sessaoId, 1, List.of(item(itemId, produtoId))));

        mockMvc.perform(get("/api/v1/sessoes/{s}/roteiro", sessaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessaoId").value(sessaoId.toString()))
                .andExpect(jsonPath("$.quantidadeItens").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(itemId.toString()))
                .andExpect(jsonPath("$.itens[0].coletado").value(false))
                .andExpect(jsonPath("$.itens[0].produto.nome").value("Tinta Acrilica Fosca Branca 18L"));
    }

    @Test
    @DisplayName("POST adiciona produto e devolve 201 com o item")
    void adicionarProduto() throws Exception {
        UUID produtoId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        when(adicionarProdutoAoRoteiroUseCase.executar(sessaoId, produtoId))
                .thenReturn(item(itemId, produtoId));

        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/itens", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":\"" + produtoId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.produtoId").value(produtoId.toString()))
                .andExpect(jsonPath("$.ordemCaminho").doesNotExist())
                .andExpect(jsonPath("$.produto.sku").value("SKU-TIN-001"));

        verify(adicionarProdutoAoRoteiroUseCase).executar(sessaoId, produtoId);
    }

    /** Primeira execucao real do handler de validacao, que existe desde o esqueleto do projeto. */
    @Test
    @DisplayName("POST sem produtoId devolve 400 apontando o campo invalido")
    void adicionarSemProdutoId() throws Exception {
        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/itens", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Falha de Validação"))
                .andExpect(jsonPath("$.validationErrors[0].field").value("produtoId"))
                .andExpect(jsonPath("$.validationErrors[0].message")
                        .value(org.hamcrest.Matchers.containsString("obrigatorio")));

        verifyNoInteractions(adicionarProdutoAoRoteiroUseCase);
    }

    @Test
    @DisplayName("POST com JSON malformado devolve 400, nao 500")
    void adicionarComJsonInvalido() throws Exception {
        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/itens", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ isso nao e json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Corpo da Requisicao Invalido"));

        verifyNoInteractions(adicionarProdutoAoRoteiroUseCase);
    }

    /** Primeira vez que o 409 do card 14 vira status HTTP. */
    @Test
    @DisplayName("sessao inativa devolve 409, nao 404")
    void sessaoInativa() throws Exception {
        when(adicionarProdutoAoRoteiroUseCase.executar(any(), any()))
                .thenThrow(new OperacaoNaoPermitidaException(
                        "Sessao %s nao esta mais ativa (status COMPLETED)".formatted(sessaoId)));

        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/itens", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Operacao Nao Permitida"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("nao esta mais ativa")));
    }

    @Test
    @DisplayName("POST de ponto de interesse devolve a rota com o desvio")
    void incluirPontoDeInteresse() throws Exception {
        UUID pontoId = UUID.randomUUID();
        when(incluirPontoDeInteresseUseCase.executar(sessaoId, TipoPonto.BANHEIRO))
                .thenReturn(new RotaCalculadaResponse(sessaoId, UUID.randomUUID(), List.of(
                        new PontoRotaResponse(1, null,
                                new PontoMapaResponse(pontoId, TipoPonto.BANHEIRO, "Sanitarios", 52, 8)))));

        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/pontos-interesse", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"BANHEIRO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pontos[0].pontoMapa.tipo").value("BANHEIRO"))
                .andExpect(jsonPath("$.pontos[0].pontoMapa.corredor").value("Sanitarios"))
                .andExpect(jsonPath("$.pontos[0].item").doesNotExist());

        verify(incluirPontoDeInteresseUseCase).executar(sessaoId, TipoPonto.BANHEIRO);
    }

    @Test
    @DisplayName("ponto de interesse sem tipo devolve 400")
    void incluirPontoSemTipo() throws Exception {
        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/pontos-interesse", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("tipo"));

        verifyNoInteractions(incluirPontoDeInteresseUseCase);
    }

    @Test
    @DisplayName("tipo que nao e de apoio devolve 409")
    void incluirPontoComTipoInvalido() throws Exception {
        when(incluirPontoDeInteresseUseCase.executar(any(), any()))
                .thenThrow(new OperacaoNaoPermitidaException(
                        "Apenas BANHEIRO ou CAIXA podem ser incluidos como ponto de apoio, e nao PRATELEIRA"));

        mockMvc.perform(post("/api/v1/sessoes/{s}/roteiro/pontos-interesse", sessaoId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tipo\":\"PRATELEIRA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("DELETE devolve 204 sem corpo")
    void removerItem() throws Exception {
        UUID itemId = UUID.randomUUID();

        mockMvc.perform(delete("/api/v1/sessoes/{s}/roteiro/itens/{i}", sessaoId, itemId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(removerProdutoDoRoteiroUseCase).executar(sessaoId, itemId);
    }

    @Test
    @DisplayName("DELETE de item inexistente devolve 404")
    void removerItemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        doThrow(new RecursoNaoEncontradoException("Item do roteiro", itemId))
                .when(removerProdutoDoRoteiroUseCase).executar(any(), any());

        mockMvc.perform(delete("/api/v1/sessoes/{s}/roteiro/itens/{i}", sessaoId, itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurso Nao Encontrado"));
    }
}
