package br.com.jence.backend.dto;

import br.com.jence.backend.factory.MapperFactory;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Remetente;

import java.time.LocalDateTime;
import java.util.UUID;

/** Espelha o schema {@code ChatMensagem} do contrato OpenAPI. */
public class ChatMensagemResponse {

    private UUID id;
    private UUID sessaoId;
    private Remetente remetente;
    private String conteudo;
    private LocalDateTime enviadoEm;

    public ChatMensagemResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getSessaoId() {
        return sessaoId;
    }

    public void setSessaoId(UUID sessaoId) {
        this.sessaoId = sessaoId;
    }

    public Remetente getRemetente() {
        return remetente;
    }

    public void setRemetente(Remetente remetente) {
        this.remetente = remetente;
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }

    public void setEnviadoEm(LocalDateTime enviadoEm) {
        this.enviadoEm = enviadoEm;
    }

    public static ChatMensagemResponse de(ChatMensagem mensagem) {
        return MapperFactory.getMapper().map(mensagem, ChatMensagemResponse.class);
    }
}
