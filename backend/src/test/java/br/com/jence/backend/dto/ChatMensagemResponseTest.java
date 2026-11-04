package br.com.jence.backend.dto;

import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Remetente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChatMensagemResponseTest {

    @Test
    @DisplayName("copia cada campo da mensagem, sem trocar os dois ids")
    void copiaOsCampos() {
        UUID id = UUID.randomUUID();
        UUID sessaoId = UUID.randomUUID();
        LocalDateTime enviadoEm = LocalDateTime.of(2026, 11, 4, 17, 30);
        ChatMensagem mensagem = ChatMensagem.reconstituir(
                id, sessaoId, Remetente.ASSISTANT, "Corredor de Tintas.", enviadoEm);

        ChatMensagemResponse resposta = ChatMensagemResponse.de(mensagem);

        assertEquals(id, resposta.getId());
        assertEquals(sessaoId, resposta.getSessaoId());
        assertEquals(Remetente.ASSISTANT, resposta.getRemetente());
        assertEquals("Corredor de Tintas.", resposta.getConteudo());
        assertEquals(enviadoEm, resposta.getEnviadoEm());
    }
}
