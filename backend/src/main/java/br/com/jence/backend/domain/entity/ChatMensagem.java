package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ChatMensagem {

    private final UUID id;
    private final UUID sessaoId;
    private final Remetente remetente;
    private final String conteudo;
    private final LocalDateTime enviadoEm;

    private ChatMensagem(UUID id, UUID sessaoId, Remetente remetente, String conteudo, LocalDateTime enviadoEm) {
        this.id = id;
        this.sessaoId = sessaoId;
        this.remetente = remetente;
        this.conteudo = conteudo;
        this.enviadoEm = enviadoEm;
    }

    public static ChatMensagem doCliente(UUID id, UUID sessaoId, String conteudo) {
        return new ChatMensagem(id, sessaoId, Remetente.USER, conteudo, LocalDateTime.now());
    }

    public static ChatMensagem doAssistente(UUID id, UUID sessaoId, String conteudo) {
        return new ChatMensagem(id, sessaoId, Remetente.ASSISTANT, conteudo, LocalDateTime.now());
    }

    public static ChatMensagem reconstituir(UUID id, UUID sessaoId, Remetente remetente,
                                            String conteudo, LocalDateTime enviadoEm) {
        return new ChatMensagem(id, sessaoId, remetente, conteudo, enviadoEm);
    }

    public boolean isDoCliente() {
        return remetente == Remetente.USER;
    }
}
