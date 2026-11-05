package br.com.jence.backend.dto;

import br.com.jence.backend.factory.MapperFactory;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import br.com.jence.backend.service.PosicaoDoCliente;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Espelha o schema {@code Sessao} do contrato OpenAPI.
 * <p>
 * Existe para que a entidade de dominio nao seja serializada diretamente: assim uma
 * refatoracao interna de {@link Sessao} nao vira mudanca de contrato de API sem querer.
 */
public class SessaoResponse {

    private UUID id;
    private StatusSessao status;
    private LocalDateTime criadoEm;
    private LocalDateTime expiracaoTtl;
    private PontoMapaResponse posicaoAtual;

    public SessaoResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StatusSessao getStatus() {
        return status;
    }

    public void setStatus(StatusSessao status) {
        this.status = status;
    }

    public LocalDateTime getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(LocalDateTime criadoEm) {
        this.criadoEm = criadoEm;
    }

    public LocalDateTime getExpiracaoTtl() {
        return expiracaoTtl;
    }

    public void setExpiracaoTtl(LocalDateTime expiracaoTtl) {
        this.expiracaoTtl = expiracaoTtl;
    }

    public PontoMapaResponse getPosicaoAtual() {
        return posicaoAtual;
    }

    public void setPosicaoAtual(PontoMapaResponse posicaoAtual) {
        this.posicaoAtual = posicaoAtual;
    }

    /** Sessao sem lista a mao: a posicao cai para a placa lida, que e o que se sabe. */
    public static SessaoResponse de(Sessao sessao) {
        return de(sessao, null);
    }

    public static SessaoResponse de(Sessao sessao, ListaRoteiro lista) {
        SessaoResponse resposta = MapperFactory.getMapper().map(sessao, SessaoResponse.class);
        resposta.setPosicaoAtual(PosicaoDoCliente.estimar(sessao, lista)
                .map(PontoMapaResponse::de)
                .orElse(null));
        return resposta;
    }
}
