package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.application.usecase.ConcluirRotaUseCase;
import br.com.jence.backend.application.usecase.ConsultarSessaoUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.RecentrarSessaoUseCase;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes da camada web: carregam apenas o controller e o tratamento de excecoes, com os casos
 * de uso mockados. Nao precisam de banco nem de credenciais.
 */
@WebMvcTest(SessaoController.class)
class SessaoControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean InicializarSessaoUseCase inicializarSessaoUseCase;
    @MockitoBean ConsultarSessaoUseCase consultarSessaoUseCase;
    @MockitoBean ConcluirRotaUseCase concluirRotaUseCase;
    @MockitoBean RecentrarSessaoUseCase recentrarSessaoUseCase;

    private SessaoResponse sessaoAtiva(UUID id) {
        LocalDateTime agora = LocalDateTime.now();
        return new SessaoResponse(id, StatusSessao.ACTIVE, agora, agora.plusMinutes(30), null);
    }

    @Test
    @DisplayName("POST /sessoes devolve 201 com Location e corpo da sessao")
    void criarSessao() throws Exception {
        UUID id = UUID.randomUUID();
        when(inicializarSessaoUseCase.executar(null)).thenReturn(sessaoAtiva(id));

        mockMvc.perform(post("/api/v1/sessoes"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/sessoes/" + id))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.criadoEm").exists())
                .andExpect(jsonPath("$.expiracaoTtl").exists());
    }

    @Test
    @DisplayName("GET /sessoes/{id} devolve 200 no formato do contrato")
    void consultarSessao() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarSessaoUseCase.executar(id)).thenReturn(sessaoAtiva(id));

        mockMvc.perform(get("/api/v1/sessoes/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("sessao inexistente vira 404 com o corpo de erro padrao")
    void sessaoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        when(consultarSessaoUseCase.executar(any()))
                .thenThrow(new RecursoNaoEncontradoException("Sessao", id));

        mockMvc.perform(get("/api/v1/sessoes/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Recurso Não Encontrado"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Sessao")))
                .andExpect(jsonPath("$.path").value("/api/v1/sessoes/" + id))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST concluir devolve a sessao COMPLETED")
    void concluirRota() throws Exception {
        UUID id = UUID.randomUUID();
        LocalDateTime agora = LocalDateTime.now();
        when(concluirRotaUseCase.executar(id)).thenReturn(
                new SessaoResponse(id, StatusSessao.COMPLETED, agora.minusMinutes(40), agora.plusMinutes(20), null));

        mockMvc.perform(post("/api/v1/sessoes/{id}/concluir", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @DisplayName("concluir sessao ja encerrada devolve 409")
    void concluirSessaoJaEncerrada() throws Exception {
        UUID id = UUID.randomUUID();
        when(concluirRotaUseCase.executar(any()))
                .thenThrow(new OperacaoNaoPermitidaException(
                        "Sessao %s nao esta mais ativa (status COMPLETED)".formatted(id)));

        mockMvc.perform(post("/api/v1/sessoes/{id}/concluir", id))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operação Não Permitida"));
    }

    // ---------------------------------------------------------------- recentrar

    @Test
    @DisplayName("PUT posicao devolve a sessao com a nova posicao")
    void recentrar() throws Exception {
        UUID id = UUID.randomUUID();
        when(recentrarSessaoUseCase.executar(id, "CEN-03")).thenReturn(sessaoAtiva(id));

        mockMvc.perform(put("/api/v1/sessoes/{id}/posicao", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoPonto\":\"CEN-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));

        verify(recentrarSessaoUseCase).executar(id, "CEN-03");
    }

    @Test
    @DisplayName("PUT posicao sem codigo devolve 400 apontando o campo")
    void recentrarSemCodigo() throws Exception {
        /*
         * Diferente de POST /sessoes, onde o codigo e opcional: aqui ele e o proprio objeto da
         * operacao, e uma requisicao sem ele nao quer dizer nada.
         */
        mockMvc.perform(put("/api/v1/sessoes/{id}/posicao", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("codigoPonto"));
    }

    @Test
    @DisplayName("PUT posicao com placa desconhecida devolve 404")
    void recentrarPlacaDesconhecida() throws Exception {
        UUID id = UUID.randomUUID();
        when(recentrarSessaoUseCase.executar(any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Placa de localizacao", "ZZZ-99"));

        mockMvc.perform(put("/api/v1/sessoes/{id}/posicao", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigoPonto\":\"ZZZ-99\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("id malformado na URL vira 400, nao 500")
    void idMalformado() throws Exception {
        mockMvc.perform(get("/api/v1/sessoes/{id}", "nao-e-um-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Parâmetro Inválido"))
                .andExpect(jsonPath("$.message")
                        .value(org.hamcrest.Matchers.containsString("nao-e-um-uuid")));
    }
}
