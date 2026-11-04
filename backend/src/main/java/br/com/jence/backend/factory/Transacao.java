package br.com.jence.backend.factory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Uma transacao com o banco, controlada so com {@code java.sql.Connection}.
 * <p>
 * Substitui o {@code @Transactional} do Spring. O uso e sempre o mesmo:
 * <pre>{@code
 * try (Transacao transacao = Transacao.iniciar()) {
 *     ... chamadas aos DAOs ...
 *     transacao.confirmar();
 * }
 * }</pre>
 * Se {@link #confirmar()} nao for chamado - porque uma excecao saiu do bloco, por exemplo -, o
 * {@link #close()} desfaz tudo. Nao ha caminho em que uma alteracao pela metade seja gravada.
 * <p>
 * Tres formas de abrir, as mesmas que o projeto usava com o Spring:
 * <ul>
 *   <li>{@link #iniciar()}: se ja houver transacao nesta thread, participa dela; senao abre uma.
 *       E o {@code REQUIRED} do Spring.</li>
 *   <li>{@link #iniciarSomenteLeitura()}: igual, mas a transacao que abrir nao aceita escrita.
 *       E o {@code readOnly = true}.</li>
 *   <li>{@link #iniciarNova()}: sempre abre uma transacao propria, com outra conexao, e deixa a
 *       de fora suspensa ate fechar. E o {@code REQUIRES_NEW}, usado no registro de ruptura
 *       (D-39), que precisa sobreviver ao rollback da operacao que o registrou.</li>
 * </ul>
 */
public class Transacao implements AutoCloseable {

    private static final ThreadLocal<Transacao> ATUAL = new ThreadLocal<>();

    /** A transacao que de fato tem a conexao. Para uma participante, e a de fora. */
    private final Transacao dona;

    /** A que estava em andamento antes desta abrir; volta a valer quando esta fechar. */
    private final Transacao anterior;

    private final Connection conexao;

    /** Dados que vivem so durante a transacao, como a foto do roteiro lido (ver ListaRoteiroDao). */
    private final Map<Object, Object> recursos;

    private boolean confirmada;
    private boolean fechada;

    /** Marcada quando uma participante fecha sem confirmar: a de fora nao pode mais gravar. */
    private boolean somenteDesfazer;

    private Transacao(Connection conexao, Transacao anterior) {
        this.dona = this;
        this.anterior = anterior;
        this.conexao = conexao;
        this.recursos = new HashMap<>();
    }

    private Transacao(Transacao dona) {
        this.dona = dona;
        this.anterior = null;
        this.conexao = dona.conexao;
        this.recursos = dona.recursos;
    }

    public static Transacao iniciar() throws SQLException {
        Transacao atual = ATUAL.get();
        return atual != null ? new Transacao(atual) : abrir(false, null);
    }

    public static Transacao iniciarSomenteLeitura() throws SQLException {
        Transacao atual = ATUAL.get();
        return atual != null ? new Transacao(atual) : abrir(true, null);
    }

    public static Transacao iniciarNova() throws SQLException {
        return abrir(false, ATUAL.get());
    }

    /** A transacao em andamento nesta thread, ou {@code null}. */
    static Transacao atual() {
        return ATUAL.get();
    }

    private static Transacao abrir(boolean somenteLeitura, Transacao anterior) throws SQLException {
        Connection conexao = ConnectionFactory.abrirConexao();
        try {
            conexao.setAutoCommit(false);
            if (somenteLeitura) {
                conexao.setReadOnly(true);
            }
        } catch (SQLException e) {
            conexao.close();
            throw e;
        }
        Transacao transacao = new Transacao(conexao, anterior);
        ATUAL.set(transacao);
        return transacao;
    }

    Connection getConexao() {
        return conexao;
    }

    /** Dados que valem so durante esta transacao. Compartilhados com as participantes. */
    public Map<Object, Object> getRecursos() {
        return recursos;
    }

    /** A transacao em andamento nesta thread. */
    public static Transacao emAndamento() {
        Transacao atual = ATUAL.get();
        if (atual == null) {
            throw new IllegalStateException("Nenhuma transacao em andamento nesta thread.");
        }
        return atual;
    }

    /**
     * Grava o que foi feito. Numa participante, so registra que ela terminou bem: quem grava e a
     * transacao de fora, quando ela propria confirmar.
     */
    public void confirmar() throws SQLException {
        if (dona != this) {
            confirmada = true;
            return;
        }
        if (somenteDesfazer) {
            throw new IllegalStateException(
                    "Uma operacao dentro desta transacao falhou; nada sera gravado.");
        }
        conexao.commit();
        confirmada = true;
    }

    /** Desfaz o que nao foi confirmado e devolve a conexao. Chamado pelo try-with-resources. */
    @Override
    public void close() throws SQLException {
        if (fechada) {
            return;
        }
        fechada = true;

        if (dona != this) {
            if (!confirmada) {
                dona.somenteDesfazer = true;
            }
            return;
        }

        try {
            if (!confirmada) {
                conexao.rollback();
            }
        } finally {
            try {
                conexao.close();
            } finally {
                if (anterior != null) {
                    ATUAL.set(anterior);
                } else {
                    ATUAL.remove();
                }
            }
        }
    }
}
