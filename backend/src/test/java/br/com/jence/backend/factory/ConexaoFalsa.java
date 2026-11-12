package br.com.jence.backend.factory;

import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Uma {@link Connection} que nao fala com banco nenhum: so anota o que recebeu.
 * <p>
 * Feita com {@link Proxy} do proprio JDK, sem biblioteca de dublê. Serve para os testes que
 * rodam sem Oracle: a {@link Transacao} abre, confirma e fecha esta conexao como faria com uma
 * de verdade, e os DAOs falsos nem chegam a usa-la.
 */
public class ConexaoFalsa {

    private final List<String> chamadas = Collections.synchronizedList(new ArrayList<>());
    private final Connection conexao;

    /** O que {@code isValid} responde. Falso por padrao, como todo metodo booleano daqui. */
    private volatile boolean valida;

    /** Metodo que, quando chamado, lanca {@link SQLException}, para simular falha do banco. */
    private volatile String metodoQueFalha;

    public ConexaoFalsa() {
        this.conexao = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    String nome = metodo.getName();
                    if (nome.equals("toString")) {
                        return "ConexaoFalsa";
                    }
                    if (nome.equals("hashCode")) {
                        return System.identityHashCode(proxy);
                    }
                    if (nome.equals("equals")) {
                        return proxy == argumentos[0];
                    }
                    chamadas.add(argumentos == null || argumentos.length == 0
                            ? nome
                            : nome + "(" + argumentos[0] + ")");
                    if (nome.equals(metodoQueFalha)) {
                        throw new SQLException("falha simulada em " + nome);
                    }
                    if (nome.equals("isValid")) {
                        return valida;
                    }
                    Class<?> retorno = metodo.getReturnType();
                    if (retorno == boolean.class) {
                        return false;
                    }
                    if (retorno == int.class) {
                        return 0;
                    }
                    return null;
                });
    }

    /** Faz {@code isValid} responder {@code true}, como uma conexao viva. */
    public ConexaoFalsa viva() {
        this.valida = true;
        return this;
    }

    /** Faz o metodo dado lancar {@link SQLException}. */
    public ConexaoFalsa falhandoEm(String metodo) {
        this.metodoQueFalha = metodo;
        return this;
    }

    public Connection getConexao() {
        return conexao;
    }

    /** As chamadas recebidas, na ordem, como {@code "setAutoCommit(false)"} ou {@code "commit"}. */
    public List<String> getChamadas() {
        return List.copyOf(chamadas);
    }

    /**
     * Faz a {@link ConnectionFactory} entregar conexoes falsas, uma nova a cada transacao. Os
     * testes que chamam isto devolvem a fabrica ao banco com {@link ConnectionFactory#usarBanco()}.
     */
    public static void usarNaFabrica() {
        ConnectionFactory.usarFonte(() -> new ConexaoFalsa().getConexao());
    }
}
