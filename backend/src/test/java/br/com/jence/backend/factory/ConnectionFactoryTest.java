package br.com.jence.backend.factory;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** O reaproveitamento de conexoes da {@link ConnectionFactory} (D-96), sem banco. */
class ConnectionFactoryTest {

    /** Cada conexao nova que a fabrica abriu, na ordem. */
    private final List<ConexaoFalsa> abertas = new ArrayList<>();

    /** O que fazer com cada conexao falsa antes de entrega-la, como marca-la viva. */
    private Consumer<ConexaoFalsa> preparar = conexao -> conexao.viva();

    /** Relogio simulado, em milissegundos. */
    private long agora = 1_000_000L;

    @BeforeEach
    void usarConexoesFalsas() {
        ConnectionFactory.definirMaximoDeGuardadas(2);
        ConnectionFactory.usarRelogio(() -> agora);
        ConnectionFactory.usarFonte(() -> {
            ConexaoFalsa conexao = new ConexaoFalsa();
            preparar.accept(conexao);
            abertas.add(conexao);
            return conexao.getConexao();
        });
    }

    @AfterEach
    void voltarAoBanco() {
        ConnectionFactory.usarBanco();
        ConnectionFactory.usarRelogio(System::currentTimeMillis);
        ConnectionFactory.definirMaximoDeGuardadas(null);
    }

    /** Roda uma transacao que confirma e devolve a conexao que ela usou. */
    private Connection transacaoConfirmada() throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            Connection usada = ConnectionFactory.getConnection();
            transacao.confirmar();
            return usada;
        }
    }

    @Test
    @DisplayName("a segunda transacao usa a conexao da primeira, sem abrir outra")
    void reaproveita() throws Exception {
        Connection primeira = transacaoConfirmada();
        Connection segunda = transacaoConfirmada();

        assertSame(primeira, segunda);
        assertEquals(1, abertas.size());
        assertEquals(List.of("setAutoCommit(false)", "commit", "setAutoCommit(false)", "commit"),
                abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("guarda no maximo o configurado; as que sobram sao fechadas")
    void respeitaOMaximo() throws Exception {
        try (Transacao a = Transacao.iniciarNova();
             Transacao b = Transacao.iniciarNova();
             Transacao c = Transacao.iniciarNova()) {
            a.confirmar();
            b.confirmar();
            c.confirmar();
        }

        assertEquals(3, abertas.size());
        assertEquals(2, ConnectionFactory.quantidadeGuardada());
        assertEquals(1, abertas.stream().filter(c -> c.getChamadas().contains("close")).count());
    }

    @Test
    @DisplayName("com maximo 0, nada e guardado: cada transacao abre e fecha a sua")
    void desligado() throws Exception {
        ConnectionFactory.definirMaximoDeGuardadas(0);

        transacaoConfirmada();
        transacaoConfirmada();

        assertEquals(2, abertas.size());
        assertEquals(List.of("setAutoCommit(false)", "commit", "close"), abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("parada ha mais de 30 s, a conexao e testada antes do uso")
    void validaDepoisDeParada() throws Exception {
        Connection primeira = transacaoConfirmada();
        agora += ConnectionFactory.VALIDAR_APOS_MS;

        assertSame(primeira, transacaoConfirmada());
        assertEquals(1, abertas.size());
        assertEquals(List.of("setAutoCommit(false)", "commit", "isValid(2)", "setAutoCommit(false)", "commit"),
                abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("se o teste falhar, a conexao morta e fechada e uma nova e aberta")
    void descartaAMorta() throws Exception {
        preparar = conexao -> { };
        Connection morta = transacaoConfirmada();
        agora += ConnectionFactory.VALIDAR_APOS_MS;

        Connection nova = transacaoConfirmada();

        assertNotSame(morta, nova);
        assertEquals(2, abertas.size());
        assertEquals(List.of("setAutoCommit(false)", "commit", "isValid(2)", "close"),
                abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("usada logo em seguida, a conexao nao e testada: o teste custaria uma ida e volta")
    void naoValidaAQuente() throws Exception {
        transacaoConfirmada();
        agora += ConnectionFactory.VALIDAR_APOS_MS - 1;

        transacaoConfirmada();

        assertEquals(List.of("setAutoCommit(false)", "commit", "setAutoCommit(false)", "commit"),
                abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("parada alem do limite, a faxina fecha a conexao e devolve a sessao ao Oracle")
    void faxinaFechaAsParadas() throws Exception {
        transacaoConfirmada();
        agora += ConnectionFactory.OCIOSIDADE_MAXIMA_MS;

        ConnectionFactory.fecharVencidas();

        assertEquals(0, ConnectionFactory.quantidadeGuardada());
        assertEquals("close", abertas.get(0).getChamadas().getLast());
    }

    @Test
    @DisplayName("uma conexao velha nao volta a ser guardada, mesmo em uso constante")
    void idadeMaxima() throws Exception {
        transacaoConfirmada();
        for (long passado = 0; passado < ConnectionFactory.IDADE_MAXIMA_MS; passado += 60_000L) {
            agora += 60_000L;
            transacaoConfirmada();
        }

        assertEquals(2, abertas.size());
        assertEquals("close", abertas.get(0).getChamadas().getLast());
    }

    @Test
    @DisplayName("somente leitura e desfeito antes de a conexao voltar a ser usada")
    void desfazSomenteLeitura() throws Exception {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            transacao.confirmar();
        }
        transacaoConfirmada();

        assertEquals(List.of("setAutoCommit(false)", "setReadOnly(true)", "commit", "setReadOnly(false)",
                "setAutoCommit(false)", "commit"), abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("se o rollback falhar, a conexao e fechada, e nao guardada")
    void falhaAoEncerrarFecha() {
        preparar = conexao -> conexao.viva().falhandoEm("rollback");

        assertThrows(SQLException.class, () -> {
            try (Transacao transacao = Transacao.iniciar()) {
                // sai sem confirmar: o close tenta desfazer, e o banco falha
            }
        });

        assertEquals(0, ConnectionFactory.quantidadeGuardada());
        assertEquals(List.of("setAutoCommit(false)", "rollback", "close"), abertas.get(0).getChamadas());
    }

    @Test
    @DisplayName("trocar a fonte fecha as guardadas, para nao misturar conexoes de origens diferentes")
    void trocarFonteFechaAsGuardadas() throws Exception {
        transacaoConfirmada();

        ConnectionFactory.usarFonte(() -> new ConexaoFalsa().getConexao());

        assertEquals(0, ConnectionFactory.quantidadeGuardada());
        assertEquals("close", abertas.get(0).getChamadas().getLast());
    }
}
