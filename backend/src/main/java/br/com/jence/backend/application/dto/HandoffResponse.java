package br.com.jence.backend.application.dto;

import java.time.LocalDateTime;

/** Espelha o schema {@code HandoffResponse} do contrato OpenAPI. */
public record HandoffResponse(
        String handoffUrl,
        String token,
        LocalDateTime tokenExpiracao
) {
}
