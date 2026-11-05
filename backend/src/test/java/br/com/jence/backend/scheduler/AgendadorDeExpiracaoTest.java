package br.com.jence.backend.scheduler;

import br.com.jence.backend.service.ExpirarSessoesInativasUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O relogio do agendador, com um caso de uso falso no lugar da varredura no banco. */
class AgendadorDeExpiracaoTest {

    /** Conta as varreduras; as primeiras falham, como se o banco estivesse fora. */
    static class VarreduraFalsa extends ExpirarSessoesInativasUseCase {
        final AtomicInteger chamadas = new AtomicInteger();
        final CountDownLatch tresChamadas = new CountDownLatch(3);
        final int falhasIniciais;

        VarreduraFalsa(int falhasIniciais) {
            super(null, null);
            this.falhasIniciais = falhasIniciais;
        }

        @Override
        public ResultadoDaVarredura executar() throws SQLException {
            int chamada = chamadas.incrementAndGet();
            tresChamadas.countDown();
            if (chamada <= falhasIniciais) {
                throw new SQLException("banco momentaneamente fora");
            }
            return new ResultadoDaVarredura(0, 0, 0);
        }
    }

    private AgendadorDeExpiracao agendador;

    @AfterEach
    void parar() {
        agendador.parar();
    }

    @Test
    @DisplayName("varre repetidas vezes, no intervalo pedido")
    void varreRepetidamente() throws Exception {
        VarreduraFalsa varredura = new VarreduraFalsa(0);
        agendador = new AgendadorDeExpiracao(varredura);

        agendador.iniciar(0, 10);

        assertTrue(varredura.tresChamadas.await(5, TimeUnit.SECONDS));
    }

    @Test
    @DisplayName("uma varredura que falha nao aposenta as seguintes")
    void falhaNaoInterrompe() throws Exception {
        VarreduraFalsa varredura = new VarreduraFalsa(2);
        agendador = new AgendadorDeExpiracao(varredura);

        agendador.iniciar(0, 10);

        assertTrue(varredura.tresChamadas.await(5, TimeUnit.SECONDS),
                "sem o try/catch, o executor pararia na primeira excecao");
    }

    @Test
    @DisplayName("depois de parar, nao varre mais")
    void pararInterrompe() throws Exception {
        VarreduraFalsa varredura = new VarreduraFalsa(0);
        agendador = new AgendadorDeExpiracao(varredura);
        agendador.iniciar(0, 10);
        assertTrue(varredura.tresChamadas.await(5, TimeUnit.SECONDS));

        agendador.parar();
        int depoisDeParar = varredura.chamadas.get();
        Thread.sleep(100);

        assertEquals(depoisDeParar, varredura.chamadas.get());
    }
}
