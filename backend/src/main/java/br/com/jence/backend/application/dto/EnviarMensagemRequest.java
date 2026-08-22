package br.com.jence.backend.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Espelha o schema {@code EnviarMensagemRequest} do contrato OpenAPI. */
public record EnviarMensagemRequest(
        @NotBlank(message = "conteudo e obrigatorio")
        @Size(max = 1000, message = "conteudo deve ter no maximo 1000 caracteres")
        String conteudo
) {
}
