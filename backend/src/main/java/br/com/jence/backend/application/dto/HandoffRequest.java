package br.com.jence.backend.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Espelha o schema {@code HandoffRequest} do contrato OpenAPI. */
public record HandoffRequest(
        @NotNull(message = "sessaoId e obrigatorio")
        UUID sessaoId
) {
}
