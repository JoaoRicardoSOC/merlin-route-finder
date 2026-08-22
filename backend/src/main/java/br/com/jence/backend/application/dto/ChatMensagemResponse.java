package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.domain.entity.Remetente;

import java.time.LocalDateTime;
import java.util.UUID;

/** Espelha o schema {@code ChatMensagem} do contrato OpenAPI. */
public record ChatMensagemResponse(
        UUID id,
        UUID sessaoId,
        Remetente remetente,
        String conteudo,
        LocalDateTime enviadoEm
) {
    public static ChatMensagemResponse de(ChatMensagem mensagem) {
        return new ChatMensagemResponse(
                mensagem.getId(),
                mensagem.getSessaoId(),
                mensagem.getRemetente(),
                mensagem.getConteudo(),
                mensagem.getEnviadoEm()
        );
    }
}
