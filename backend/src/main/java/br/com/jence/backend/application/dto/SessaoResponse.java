package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.service.PosicaoDoCliente;

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
        LocalDateTime expiracaoTtl,
        PontoMapaResponse posicaoAtual
) {
    /** Sessao sem lista a mao: a posicao cai para a placa lida, que e o que se sabe. */
    public static SessaoResponse de(Sessao sessao) {
        return de(sessao, null);
    }

    public static SessaoResponse de(Sessao sessao, ListaRoteiro lista) {
        return new SessaoResponse(
                sessao.getId(),
                sessao.getStatus(),
                sessao.getCriadoEm(),
                sessao.getExpiracaoTtl(),
                PosicaoDoCliente.estimar(sessao, lista)
                        .map(PontoMapaResponse::de)
                        .orElse(null)
        );
    }
}
