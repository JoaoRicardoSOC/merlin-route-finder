package br.com.jence.backend.application.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code POST /handoff/validate}.
 * <p>
 * O token viajava na query string ate a Fase 3. Passou para o corpo por dois motivos que se
 * somam: URL fica gravada em historico de navegador e em log de servidor, e a validacao
 * <b>consome</b> o token, o que nunca deveria ter sido um GET. Ver D-44.
 */
public record ValidarHandoffRequest(
        @NotBlank(message = "token e obrigatorio")
        String token
) {
}
