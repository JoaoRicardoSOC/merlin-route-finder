package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class Sessao {

    public static final Duration TTL_PADRAO = Duration.ofMinutes(30);

    private final UUID id;
    private StatusSessao status;
    private final LocalDateTime criadoEm;
    private LocalDateTime expiracaoTtl;

    private Sessao(UUID id, StatusSessao status, LocalDateTime criadoEm, LocalDateTime expiracaoTtl) {
        this.id = id;
        this.status = status;
        this.criadoEm = criadoEm;
        this.expiracaoTtl = expiracaoTtl;
    }

    public static Sessao iniciar(UUID id) {
        LocalDateTime agora = LocalDateTime.now();
        return new Sessao(id, StatusSessao.ACTIVE, agora, agora.plus(TTL_PADRAO));
    }

    public static Sessao reconstituir(UUID id, StatusSessao status, LocalDateTime criadoEm, LocalDateTime expiracaoTtl) {
        return new Sessao(id, status, criadoEm, expiracaoTtl);
    }

    public boolean isValida() {
        return isValida(LocalDateTime.now());
    }

    public boolean isValida(LocalDateTime referencia) {
        return status == StatusSessao.ACTIVE && referencia.isBefore(expiracaoTtl);
    }

    public void encerrar() {
        finalizarCom(StatusSessao.COMPLETED);
    }

    public void expirar() {
        finalizarCom(StatusSessao.EXPIRED);
    }

    public void abandonar() {
        finalizarCom(StatusSessao.ABANDONED);
    }

    public void renovarSessao() {
        if (!isValida()) {
            return;
        }
        this.expiracaoTtl = LocalDateTime.now().plus(TTL_PADRAO);
    }

    private void finalizarCom(StatusSessao statusFinal) {
        if (status != StatusSessao.ACTIVE) {
            return;
        }
        this.status = statusFinal;
    }
}
