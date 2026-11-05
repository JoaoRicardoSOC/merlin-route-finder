package br.com.jence.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Espelha o schema {@code EnviarMensagemRequest} do contrato OpenAPI. */
public class EnviarMensagemRequest {

    @NotBlank(message = "conteudo e obrigatorio")
    @Size(max = 1000, message = "conteudo deve ter no maximo 1000 caracteres")
    private String conteudo;

    public EnviarMensagemRequest() {
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }
}
