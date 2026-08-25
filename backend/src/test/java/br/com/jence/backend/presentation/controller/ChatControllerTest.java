package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.application.usecase.ConsultarHistoricoChatUseCase;
import br.com.jence.backend.application.usecase.ConversarComAssistenteUseCase;
import br.com.jence.backend.domain.entity.Remetente;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ChatController.class)
class ChatControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean ConversarComAssistenteUseCase conversarComAssistenteUseCase;
    @MockitoBean ConsultarHistoricoChatUseCase consultarHistoricoChatUseCase;

    private UUID sessaoId;

    @BeforeEach
    void preparar() {
        sessaoId = UUID.randomUUID();
    }

    private ChatMensagemResponse mensagem(Remetente remetente, String conteudo) {
        return new ChatMensagemResponse(UUID.randomUUID(), sessaoId, remetente, conteudo,
                LocalDateTime.now());
    }

    private MockHttpServletRequestBuilder enviar(String corpo) {
        return post("/api/v1/sessoes/{s}/chat/mensagens", sessaoId)
                .contentType(APPLICATION_JSON)
                .content(corpo);
    }

    // ---------------------------------------------------------------- envio

    @Test
    @DisplayName("POST devolve 201 com a resposta do assistente, nao com a pergunta")
    void enviarMensagem() throws Exception {
        when(conversarComAssistenteUseCase.executar(eq(sessaoId), any())).thenReturn(
                mensagem(Remetente.ASSISTANT,
                        "Voce vai precisar de tinta, rolo e lixa. Tudo no corredor Tintas."));

        mockMvc.perform(enviar("""
                        {"conteudo": "o que preciso para pintar uma parede?"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.remetente").value("ASSISTANT"))
                .andExpect(jsonPath("$.sessaoId").value(sessaoId.toString()))
                .andExpect(jsonPath("$.conteudo").value(
                        org.hamcrest.Matchers.containsString("corredor Tintas")))
                .andExpect(jsonPath("$.enviadoEm").exists());
    }

    @Test
    @DisplayName("o conteudo enviado chega intacto ao caso de uso")
    void repassaOConteudo() throws Exception {
        when(conversarComAssistenteUseCase.executar(any(), any()))
                .thenReturn(mensagem(Remetente.ASSISTANT, "resposta"));

        mockMvc.perform(enviar("""
                        {"conteudo": "quanto custa a torneira cromada?"}"""))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(conversarComAssistenteUseCase).executar(eq(sessaoId), captor.capture());

        assertThat(captor.getValue()).isEqualTo("quanto custa a torneira cromada?");
    }

    @Test
    @DisplayName("conteudo em branco devolve 400 sem gastar chamada de IA")
    void conteudoEmBranco() throws Exception {
        mockMvc.perform(enviar("""
                        {"conteudo": "   "}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("conteudo"));

        verify(conversarComAssistenteUseCase, never()).executar(any(), any());
    }

    @Test
    @DisplayName("conteudo acima do limite devolve 400 sem gastar chamada de IA")
    void conteudoLongoDemais() throws Exception {
        String gigante = "a".repeat(1001);

        mockMvc.perform(enviar("{\"conteudo\": \"" + gigante + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("conteudo"));

        verify(conversarComAssistenteUseCase, never()).executar(any(), any());
    }

    @Test
    @DisplayName("sessao encerrada devolve 409")
    void sessaoInativa() throws Exception {
        when(conversarComAssistenteUseCase.executar(any(), any())).thenThrow(
                new OperacaoNaoPermitidaException("Sessao nao esta mais ativa (status COMPLETED)"));

        mockMvc.perform(enviar("""
                        {"conteudo": "oi"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operação Não Permitida"));
    }

    @Test
    @DisplayName("sessao inexistente devolve 404")
    void sessaoInexistente() throws Exception {
        when(conversarComAssistenteUseCase.executar(any(), any()))
                .thenThrow(new RecursoNaoEncontradoException("Sessao", sessaoId));

        mockMvc.perform(enviar("""
                        {"conteudo": "oi"}"""))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("corpo ilegivel devolve 400, nao 500")
    void corpoIlegivel() throws Exception {
        mockMvc.perform(enviar("{isso nao e json"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- historico

    @Test
    @DisplayName("GET devolve a conversa na ordem em que aconteceu")
    void historicoEmOrdem() throws Exception {
        when(consultarHistoricoChatUseCase.executar(sessaoId)).thenReturn(List.of(
                mensagem(Remetente.USER, "quero reformar o banheiro"),
                mensagem(Remetente.ASSISTANT, "comece pela torneira e pelo sifao"),
                mensagem(Remetente.USER, "e a parte hidraulica?"),
                mensagem(Remetente.ASSISTANT, "cano PVC e cola, no corredor Encanamento")));

        mockMvc.perform(get("/api/v1/sessoes/{s}/chat/mensagens", sessaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].remetente").value("USER"))
                .andExpect(jsonPath("$[0].conteudo").value("quero reformar o banheiro"))
                .andExpect(jsonPath("$[1].remetente").value("ASSISTANT"))
                .andExpect(jsonPath("$[3].conteudo").value(
                        org.hamcrest.Matchers.containsString("Encanamento")));
    }

    @Test
    @DisplayName("conversa que ainda nao comecou devolve 200 com lista vazia")
    void historicoVazio() throws Exception {
        when(consultarHistoricoChatUseCase.executar(sessaoId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/sessoes/{s}/chat/mensagens", sessaoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("sessao inexistente devolve 404, distinguivel de conversa vazia")
    void historicoDeSessaoInexistente() throws Exception {
        when(consultarHistoricoChatUseCase.executar(sessaoId))
                .thenThrow(new RecursoNaoEncontradoException("Sessao", sessaoId));

        mockMvc.perform(get("/api/v1/sessoes/{s}/chat/mensagens", sessaoId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurso Não Encontrado"));
    }

    @Test
    @DisplayName("sessaoId malformado devolve 400, nao 500")
    void sessaoIdMalformado() throws Exception {
        mockMvc.perform(get("/api/v1/sessoes/{s}/chat/mensagens", "nao-e-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parâmetro Inválido"));
    }
}
