package br.com.jence.backend.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Regras da {@link Transacao}, verificadas sem banco, com conexoes falsas. */
class TransacaoTest {

    /** Cada transacao nova recebe a proxima conexao falsa desta fila. */
    private final Deque<ConexaoFalsa> abertas = new ArrayDeque<>();

    @BeforeEach
    void usarConexoesFalsas() {
        ConnectionFactory.usarFonte(() -> {
            ConexaoFalsa conexao = new ConexaoFalsa();
            abertas.add(conexao);
            return conexao.getConexao();
        });
    }

    @AfterEach
    void voltarAoBanco() {
        ConnectionFactory.usarBanco();
    }

    @Test
    @DisplayName("confirmar grava e fechar devolve a conexao")
    void confirmarGrava() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            transacao.confirmar();
        }

        assertEquals(List.of("setAutoCommit(false)", "commit", "close"), abertas.getFirst().getChamadas());
    }

    @Test
    @DisplayName("fechar sem confirmar desfaz, inclusive quando uma excecao sai do bloco")
    void excecaoDesfaz() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (Transacao transacao = Transacao.iniciar()) {
                throw new IllegalArgumentException("falha no meio da operacao");
            }
        });

        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), abertas.getFirst().getChamadas());
    }

    @Test
    @DisplayName("somente leitura marca a conexao como tal")
    void somenteLeitura() throws Exception {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            transacao.confirmar();
        }

        assertEquals(List.of("setAutoCommit(false)", "setReadOnly(true)", "commit", "close"),
                abertas.getFirst().getChamadas());
    }

    @Test
    @DisplayName("transacao aberta dentro de outra participa dela: mesma conexao, um so commit")
    void participa() throws Exception {
        try (Transacao fora = Transacao.iniciar()) {
            Connection deFora = ConnectionFactory.getConnection();
            try (Transacao dentro = Transacao.iniciar()) {
                assertSame(deFora, ConnectionFactory.getConnection());
                dentro.confirmar();
            }
            fora.confirmar();
        }

        assertEquals(1, abertas.size());
        assertEquals(List.of("setAutoCommit(false)", "commit", "close"), abertas.getFirst().getChamadas());
    }

    @Test
    @DisplayName("se a de dentro falhar, a de fora nao pode mais gravar")
    void falhaDentroImpedeGravarFora() throws Exception {
        try (Transacao fora = Transacao.iniciar()) {
            try (Transacao dentro = Transacao.iniciar()) {
                // fecha sem confirmar, como faria uma excecao capturada mais acima
            }
            assertThrows(IllegalStateException.class, fora::confirmar);
        }

        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), abertas.getFirst().getChamadas());
    }

    @Test
    @DisplayName("transacao nova usa outra conexao e, ao fechar, a de fora volta a valer")
    void transacaoNova() throws Exception {
        try (Transacao fora = Transacao.iniciar()) {
            Connection deFora = ConnectionFactory.getConnection();
            try (Transacao nova = Transacao.iniciarNova()) {
                assertNotSame(deFora, ConnectionFactory.getConnection());
                nova.confirmar();
            }
            assertSame(deFora, ConnectionFactory.getConnection());
            // a de fora desfaz; a nova ja confirmou e nao e afetada
        }

        assertEquals(2, abertas.size());
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), abertas.getFirst().getChamadas());
        assertEquals(List.of("setAutoCommit(false)", "commit", "close"), abertas.getLast().getChamadas());
    }

    @Test
    @DisplayName("pedir conexao fora de transacao e erro de programacao, e falha na hora")
    void foraDeTransacao() {
        IllegalStateException erro = assertThrows(IllegalStateException.class, ConnectionFactory::getConnection);
        assertTrue(erro.getMessage().contains("Transacao.iniciar"));
        assertTrue(abertas.isEmpty());
    }

    @Test
    @DisplayName("depois de fechar, a thread fica sem transacao")
    void fecharLimpaAThread() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            transacao.confirmar();
        }

        assertThrows(IllegalStateException.class, ConnectionFactory::getConnection);
    }
}
