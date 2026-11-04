package br.com.jence.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O tempo de vida da sessao, que deixou de ser um detalhe de infraestrutura.
 * <p>
 * Enquanto havia totem, o TTL servia para <b>liberar o equipamento</b>: perder a sessao era
 * incomodo, mas a lista podia ser refeita ali mesmo. Agora a sessao guarda a lista inteira, e
 * perde-la significa mandar o cliente comecar de novo no meio de uma loja.
 */
class SessaoTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 25, 10, 0);

    private Sessao recemCriada() {
        return Sessao.iniciar(UUID.randomUUID());
    }

    // ---------------------------------------------------------------- o TTL novo

    @Test
    @DisplayName("uma sessao parada sobrevive muito alem dos 30 minutos antigos")
    void sobreviveAosTrintaMinutos() {
        /*
         * O caso concreto que motivou a mudanca: o cliente atende uma ligacao de quarenta
         * minutos no meio da loja. Com o TTL antigo, ele voltava para uma lista vazia.
         */
        Sessao sessao = recemCriada();

        assertTrue(sessao.isValida(LocalDateTime.now().plusMinutes(40)));
        assertTrue(sessao.isValida(LocalDateTime.now().plusHours(3)));
    }

    @Test
    @DisplayName("mas ela ainda vence: nao virou sessao eterna")
    void aindaVence() {
        // Sem vencimento, o banco acumularia sessoes ACTIVE para sempre e a varredura da D-42
        // nao teria o que classificar.
        assertFalse(recemCriada().isValida(LocalDateTime.now().plusHours(5)));
    }

    @Test
    @DisplayName("o TTL declarado e o que a sessao de fato usa")
    void ttlCoerente() {
        Sessao sessao = recemCriada();

        assertEquals(Sessao.TTL_PADRAO,
                Duration.between(sessao.getCriadoEm(), sessao.getExpiracaoTtl()));
    }

    // ---------------------------------------------------------------- renovacao

    @Test
    @DisplayName("interagir empurra o vencimento para frente")
    void renovarEmpurraOVencimento() {
        Sessao sessao = Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                AGORA.minusHours(3), LocalDateTime.now().plusMinutes(5), null, null);

        sessao.renovarSessao();

        assertTrue(sessao.isValida(LocalDateTime.now().plusHours(3)));
    }

    @Test
    @DisplayName("sessao ja vencida nao ressuscita por renovacao")
    void vencidaNaoRessuscita() {
        /*
         * Renovar e sinal de atividade, nao um jeito de desfazer o vencimento: se a sessao ja
         * morreu, a proxima acao do cliente precisa receber a recusa e nao um sistema que
         * finge que nada aconteceu.
         */
        Sessao vencida = Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                AGORA.minusHours(9), LocalDateTime.now().minusMinutes(1), null, null);

        vencida.renovarSessao();

        assertFalse(vencida.isValida());
    }

    @Test
    @DisplayName("sessao encerrada nao volta a valer, mesmo dentro do prazo")
    void encerradaNaoVolta() {
        Sessao sessao = recemCriada();
        sessao.encerrar();

        assertFalse(sessao.isValida());
        assertEquals(StatusSessao.COMPLETED, sessao.getStatus());
    }
}
