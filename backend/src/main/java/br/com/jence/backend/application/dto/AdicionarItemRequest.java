package br.com.jence.backend.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Espelha o schema {@code AdicionarItemRequest} do contrato OpenAPI. */
public record AdicionarItemRequest(
        @NotNull(message = "produtoId e obrigatorio")
        UUID produtoId
) {
}
