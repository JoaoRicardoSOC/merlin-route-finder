package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Espelha o schema {@code Sessao} do contrato OpenAPI.
 * <p>
 * Existe para que a entidade de dominio nao seja serializada diretamente: assim uma
 * refatoracao interna de {@link Sessao} nao vira mudanca de contrato de API sem querer.
 */
public record SessaoResponse(
        UUID id,
        StatusSessao status,
        LocalDateTime criadoEm,
        LocalDateTime expiracaoTtl
) {
    public static SessaoResponse de(Sessao sessao) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getStatus(),
                sessao.getCriadoEm(),
                sessao.getExpiracaoTtl()
        );
    }
}
