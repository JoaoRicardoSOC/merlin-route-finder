package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.*;
import br.com.jence.backend.application.usecase.GerarHandoffUseCase;
import br.com.jence.backend.application.usecase.ValidarHandoffUseCase;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.TokenHandoffExpiradoException;
import br.com.jence.backend.domain.exception.TokenHandoffInvalidoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HandoffController.class)
class HandoffControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean GerarHandoffUseCase gerarHandoffUseCase;
    @MockitoBean ValidarHandoffUseCase validarHandoffUseCase;

    private final UUID sessaoId = UUID.randomUUID();

    private PontoRotaResponse ponto(int ordem, String corredor, int x, int y) {
        UUID produtoId = UUID.randomUUID();
        UUID pontoId = UUID.randomUUID();
        return new PontoRotaResponse(ordem,
                new ItemRoteiroDetalhadoResponse(UUID.randomUUID(), produtoId, ordem, false,
                        new ProdutoResponse(produtoId, "SKU-X", "Produto " + ordem,
                                new BigDecimal("10.00"), 5, pontoId)),
                new PontoMapaResponse(pontoId, TipoPonto.PRATELEIRA, corredor, x, y));
    }

    @Test
    @DisplayName("POST /handoff devolve 201 com URL do QR, token e expiracao")
    void gerarHandoff() throws Exception {
        when(gerarHandoffUseCase.executar(sessaoId)).thenReturn(new HandoffResponse(
                "http://localhost:5173/rota?token=jwt-fake", "jwt-fake", LocalDateTime.now().plusMinutes(5)));

        mockMvc.perform(post("/api/v1/handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessaoId\":\"" + sessaoId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("jwt-fake"))
                .andExpect(jsonPath("$.handoffUrl").value(org.hamcrest.Matchers.containsString("token=jwt-fake")))
                .andExpect(jsonPath("$.tokenExpiracao").exists());

        verify(gerarHandoffUseCase).executar(sessaoId);
    }

    @Test
    @DisplayName("POST sem sessaoId devolve 400 apontando o campo")
    void gerarSemSessaoId() throws Exception {
        mockMvc.perform(post("/api/v1/handoff")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("sessaoId"));

        verifyNoInteractions(gerarHandoffUseCase);
    }

    @Test
    @DisplayName("lista vazia devolve 409")
    void gerarComListaVazia() throws Exception {
        when(gerarHandoffUseCase.executar(any()))
                .thenThrow(new OperacaoNaoPermitidaException("Nao ha o que roteirizar: a lista esta vazia"));

        mockMvc.perform(post("/api/v1/handoff")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sessaoId\":\"" + sessaoId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("vazia")));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder validar(String corpo) {
        return post("/api/v1/handoff/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(corpo);
    }

    @Test
    @DisplayName("POST /validate devolve a rota com pontos ordenados e coordenadas")
    void validarToken() throws Exception {
        when(validarHandoffUseCase.executar("jwt-fake")).thenReturn(new RotaCalculadaResponse(
                sessaoId, UUID.randomUUID(),
                List.of(ponto(1, "Materiais de construcao", 14, 80), ponto(2, "Tintas", 32, 10))));

        mockMvc.perform(validar("{\"token\":\"jwt-fake\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessaoId").value(sessaoId.toString()))
                .andExpect(jsonPath("$.pontos.length()").value(2))
                .andExpect(jsonPath("$.pontos[0].ordem").value(1))
                .andExpect(jsonPath("$.pontos[0].pontoMapa.corredor").value("Materiais de construcao"))
                .andExpect(jsonPath("$.pontos[0].pontoMapa.coordenadaX").value(14))
                .andExpect(jsonPath("$.pontos[1].ordem").value(2))
                .andExpect(jsonPath("$.pontos[0].item.coletado").value(false));
    }

    /** Primeira vez que o 401 do card 16 vira status HTTP. */
    @Test
    @DisplayName("token invalido ou ja usado devolve 401")
    void validarTokenInvalido() throws Exception {
        when(validarHandoffUseCase.executar(any()))
                .thenThrow(new TokenHandoffInvalidoException("Token de handoff invalido ou ja utilizado"));

        mockMvc.perform(validar("{\"token\":\"qualquer\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("Token de Handoff Invalido"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("invalido")));
    }

    @Test
    @DisplayName("token expirado devolve 401 com rotulo proprio, para o Totem oferecer um QR novo")
    void validarTokenExpirado() throws Exception {
        when(validarHandoffUseCase.executar(any()))
                .thenThrow(new TokenHandoffExpiradoException("Token de handoff expirado"));

        mockMvc.perform(validar("{\"token\":\"jwt-vencido\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error")
                        .value("Token de Handoff Expirado"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("sua lista continua montada")));
    }

    @Test
    @DisplayName("POST /validate sem token no corpo devolve 400 apontando o campo")
    void validarSemToken() throws Exception {
        mockMvc.perform(validar("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("token"));

        verifyNoInteractions(validarHandoffUseCase);
    }

    @Test
    @DisplayName("token em branco devolve 400 sem chegar ao caso de uso")
    void validarTokenEmBranco() throws Exception {
        mockMvc.perform(validar("{\"token\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("token"));

        verifyNoInteractions(validarHandoffUseCase);
    }

    @Test
    @DisplayName("o token nao e mais aceito na query string")
    void tokenNaQueryStringNaoFuncionaMais() throws Exception {
        // A garantia do hardening: o caminho antigo deixou de existir, em vez de continuar
        // funcionando em paralelo - o que anularia o proposito da mudanca.
        mockMvc.perform(get("/api/v1/handoff/validate").param("token", "jwt-fake"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(validarHandoffUseCase);
    }
}
