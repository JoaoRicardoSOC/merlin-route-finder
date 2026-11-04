package br.com.jence.backend.factory;

import br.com.jence.backend.config.Configuracao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Fabrica de conexoes com o Oracle, no formato do material.
 * <p>
 * Duas diferencas em relacao ao {@code ConnectionFactory} do material, ambas obrigatorias:
 * <ul>
 *   <li>URL, usuario e senha vem das variaveis de ambiente, e nao de constantes no codigo. O
 *       repositorio e publico.</li>
 *   <li>{@link #getConnection()} devolve a conexao da {@link Transacao} em andamento, e nao uma
 *       conexao nova a cada chamada. E o que permite a varios DAOs gravarem na mesma transacao,
 *       e o que garante que toda conexao aberta e fechada: quem abre e fecha e a transacao.</li>
 * </ul>
 * Sem pool de conexoes, como no material: cada transacao abre a sua e a fecha no fim. O limite
 * de 20 sessoes por usuario do schema da FIAP (D-46) continua respeitado porque nenhuma conexao
 * sobrevive a transacao que a abriu.
 */
public class ConnectionFactory {

    /** De onde vem uma conexao nova. Trocavel nos testes, que rodam sem banco. */
    @FunctionalInterface
    public interface FonteDeConexao {
        Connection abrir() throws SQLException;
    }

    private static final FonteDeConexao BANCO = ConnectionFactory::conectarAoBanco;

    private static volatile FonteDeConexao fonte = BANCO;

    private ConnectionFactory() {
    }

    /**
     * A conexao da transacao em andamento.
     *
     * @throws IllegalStateException se nao houver transacao aberta nesta thread. Todo acesso ao
     *                               banco passa por uma {@link Transacao}; um DAO chamado fora
     *                               dela e erro de programacao, e falhar aqui e melhor do que
     *                               abrir uma conexao que ninguem fecharia.
     */
    public static Connection getConnection() {
        Transacao atual = Transacao.atual();
        if (atual == null) {
            throw new IllegalStateException(
                    "Acesso ao banco fora de uma transacao. Abra uma com Transacao.iniciar().");
        }
        return atual.getConexao();
    }

    /** Abre uma conexao nova. Usado so pela {@link Transacao}. */
    static Connection abrirConexao() throws SQLException {
        return fonte.abrir();
    }

    /** Troca a fonte de conexoes. Usado pelos testes que rodam sem banco. */
    public static void usarFonte(FonteDeConexao novaFonte) {
        fonte = novaFonte;
    }

    /** Volta a abrir conexoes com o banco de verdade. */
    public static void usarBanco() {
        fonte = BANCO;
    }

    private static Connection conectarAoBanco() throws SQLException {
        Configuracao configuracao = Configuracao.doAmbiente();
        return DriverManager.getConnection(
                configuracao.getDbUrl(), configuracao.getDbUsuario(), configuracao.getDbSenha());
    }
}
