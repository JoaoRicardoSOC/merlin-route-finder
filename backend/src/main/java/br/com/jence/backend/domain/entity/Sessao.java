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

    /**
     * A placa em que o cliente entrou, ou a ultima em que ele se recentrou.
     * <p>
     * Pode ser nula: se o codigo informado nao corresponder a placa alguma, a sessao nasce
     * assim mesmo. Um mapa sem "voce esta aqui" ainda serve; recusar a sessao nao serviria.
     */
    private PontoMapa pontoEscaneado;

    /** Quando o ponto acima foi lido. Nulo junto com ele. */
    private LocalDateTime escaneadoEm;

    private Sessao(UUID id, StatusSessao status, LocalDateTime criadoEm, LocalDateTime expiracaoTtl,
                   PontoMapa pontoEscaneado, LocalDateTime escaneadoEm) {
        this.id = id;
        this.status = status;
        this.criadoEm = criadoEm;
        this.expiracaoTtl = expiracaoTtl;
        this.pontoEscaneado = pontoEscaneado;
        this.escaneadoEm = escaneadoEm;
    }

    /** Sessao sem posicao: o cliente chegou a pagina sem informar placa nenhuma. */
    public static Sessao iniciar(UUID id) {
        return iniciar(id, null);
    }

    public static Sessao iniciar(UUID id, PontoMapa pontoEscaneado) {
        LocalDateTime agora = LocalDateTime.now();
        return new Sessao(id, StatusSessao.ACTIVE, agora, agora.plus(TTL_PADRAO),
                pontoEscaneado, pontoEscaneado == null ? null : agora);
    }

    public static Sessao reconstituir(UUID id, StatusSessao status, LocalDateTime criadoEm,
                                      LocalDateTime expiracaoTtl, PontoMapa pontoEscaneado,
                                      LocalDateTime escaneadoEm) {
        return new Sessao(id, status, criadoEm, expiracaoTtl, pontoEscaneado, escaneadoEm);
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

    /**
     * O cliente se perdeu e leu outra placa.
     * <p>
     * Nao mexe na lista nem no que ja foi coletado: recentrar diz onde ele esta agora, e nao
     * desfaz nada do que ele fez. Como {@code escaneadoEm} passa a ser o instante mais
     * recente, a nova placa vence o ultimo item coletado na estimativa de posicao.
     *
     * @see br.com.jence.backend.domain.service.PosicaoDoCliente
     */
    public void recentrarEm(PontoMapa ponto, LocalDateTime quando) {
        this.pontoEscaneado = ponto;
        this.escaneadoEm = quando;
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
