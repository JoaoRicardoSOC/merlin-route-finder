package br.com.jence.backend.factory;

import br.com.jence.backend.config.Configuracao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Fabrica de conexoes com o Oracle, no formato do material.
 * <p>
 * Tres diferencas em relacao ao {@code ConnectionFactory} do material:
 * <ul>
 *   <li>URL, usuario e senha vem das variaveis de ambiente, e nao de constantes no codigo. O
 *       repositorio e publico.</li>
 *   <li>{@link #getConnection()} devolve a conexao da {@link Transacao} em andamento, e nao uma
 *       conexao nova a cada chamada. E o que permite a varios DAOs gravarem na mesma transacao,
 *       e o que garante que toda conexao aberta e devolvida: quem pega e devolve e a transacao.</li>
 *   <li><b>Conexoes sao reaproveitadas entre transacoes</b> (D-96). Abrir uma conexao com o
 *       Oracle da FIAP custa cerca de onze idas e voltas na rede; do Render, nos EUA, isso passa
 *       de um segundo por requisicao. Ao fim da transacao, a conexao fica guardada para a
 *       proxima, em vez de ser fechada. So com {@code java.util}, sem biblioteca de pool.</li>
 * </ul>
 * Os limites vem do schema da FIAP (D-46): 20 sessoes por usuario e 30 minutos de
 * {@code IDLE_TIME}. Por isso poucas conexoes ficam guardadas, nenhuma fica parada por muito
 * tempo e nenhuma vive o bastante para o Oracle derruba-la por conta propria.
 */
public class ConnectionFactory {

    /** De onde vem uma conexao nova. Trocavel nos testes, que rodam sem banco. */
    @FunctionalInterface
    public interface FonteDeConexao {
        Connection abrir() throws SQLException;
    }

    /**
     * Vida total de uma conexao. Abaixo dos 30 minutos de {@code IDLE_TIME} do Oracle, para que
     * a conexao morra do nosso lado, e nao derrubada pelo servidor no meio de uma requisicao.
     */
    static final long IDADE_MAXIMA_MS = 25 * 60_000L;

    /**
     * Guardada sem uso por mais que isto, a conexao e fechada e a sessao volta para a cota. Uma
     * maquina de desenvolvimento parada no almoco nao segura sessoes de quem esta publicado.
     */
    static final long OCIOSIDADE_MAXIMA_MS = 5 * 60_000L;

    /**
     * Parada por mais que isto, a conexao e testada antes de voltar ao uso. O teste custa uma ida
     * e volta; abrir uma nova custaria onze.
     */
    static final long VALIDAR_APOS_MS = 30_000L;

    /** De quanto em quanto tempo as guardadas vencidas sao fechadas, mesmo sem movimento. */
    private static final long FAXINA_INTERVALO_MS = 60_000L;

    /** Uma conexao guardada, com o que e preciso para saber se ainda serve. */
    private record Guardada(Connection conexao, long criadaEm, long devolvidaEm) {
    }

    private static final FonteDeConexao BANCO = ConnectionFactory::conectarAoBanco;

    private static volatile FonteDeConexao fonte = BANCO;

    /** A mais recente na frente: as usadas com frequencia ficam quentes, as outras envelhecem e saem. */
    private static final Deque<Guardada> GUARDADAS = new ArrayDeque<>();

    /** Quando cada conexao aberta por esta fabrica nasceu, esteja ela em uso ou guardada. */
    private static final Map<Connection, Long> CRIADAS_EM = new IdentityHashMap<>();

    /** Lido da configuracao no primeiro uso; trocavel nos testes. */
    private static Integer maximoDeGuardadas;

    private static LongSupplier relogio = System::currentTimeMillis;

    private static ScheduledExecutorService faxina;

    private ConnectionFactory() {
    }

    /**
     * A conexao da transacao em andamento.
     *
     * @throws IllegalStateException se nao houver transacao aberta nesta thread. Todo acesso ao
     *                               banco passa por uma {@link Transacao}; um DAO chamado fora
     *                               dela e erro de programacao, e falhar aqui e melhor do que
     *                               abrir uma conexao que ninguem devolveria.
     */
    public static Connection getConnection() {
        Transacao atual = Transacao.atual();
        if (atual == null) {
            throw new IllegalStateException(
                    "Acesso ao banco fora de uma transacao. Abra uma com Transacao.iniciar().");
        }
        return atual.getConexao();
    }

    /**
     * Uma conexao para a {@link Transacao}: a guardada mais recente que ainda sirva, ou uma
     * nova. Quem recebe devolve com {@link #devolverConexao}.
     */
    static Connection abrirConexao() throws SQLException {
        while (true) {
            Guardada guardada;
            synchronized (ConnectionFactory.class) {
                guardada = GUARDADAS.pollFirst();
            }
            if (guardada == null) {
                break;
            }
            long agora = relogio.getAsLong();
            if (vencida(guardada, agora)
                    || (agora - guardada.devolvidaEm() >= VALIDAR_APOS_MS && !valida(guardada.conexao()))) {
                fechar(guardada.conexao());
                continue;
            }
            return guardada.conexao();
        }

        Connection nova = fonte.abrir();
        synchronized (ConnectionFactory.class) {
            CRIADAS_EM.put(nova, relogio.getAsLong());
        }
        return nova;
    }

    /**
     * Recebe de volta a conexao de uma transacao que terminou.
     *
     * @param reaproveitavel {@code false} quando algo falhou ao encerrar a transacao: a conexao
     *                       pode estar num estado que ninguem conhece, e e fechada.
     */
    static void devolverConexao(Connection conexao, boolean reaproveitavel) {
        long agora = relogio.getAsLong();
        synchronized (ConnectionFactory.class) {
            Long criadaEm = CRIADAS_EM.get(conexao);
            if (reaproveitavel && criadaEm != null
                    && agora - criadaEm < IDADE_MAXIMA_MS
                    && GUARDADAS.size() < maximoDeGuardadas()) {
                GUARDADAS.addFirst(new Guardada(conexao, criadaEm, agora));
                iniciarFaxina();
                return;
            }
        }
        fechar(conexao);
    }

    /** Fecha as guardadas que passaram do prazo. Roda sozinha a cada minuto. */
    static void fecharVencidas() {
        long agora = relogio.getAsLong();
        List<Connection> vencidas = new ArrayList<>();
        synchronized (ConnectionFactory.class) {
            Iterator<Guardada> iterador = GUARDADAS.iterator();
            while (iterador.hasNext()) {
                Guardada guardada = iterador.next();
                if (vencida(guardada, agora)) {
                    iterador.remove();
                    vencidas.add(guardada.conexao());
                }
            }
        }
        vencidas.forEach(ConnectionFactory::fechar);
    }

    /** Fecha todas as guardadas. Chamado no desligamento e quando a fonte muda. */
    public static void fecharGuardadas() {
        List<Connection> todas = new ArrayList<>();
        synchronized (ConnectionFactory.class) {
            GUARDADAS.forEach(guardada -> todas.add(guardada.conexao()));
            GUARDADAS.clear();
        }
        todas.forEach(ConnectionFactory::fechar);
    }

    /** Quantas conexoes estao guardadas agora. */
    static synchronized int quantidadeGuardada() {
        return GUARDADAS.size();
    }

    /** Troca a fonte de conexoes. Usado pelos testes que rodam sem banco. */
    public static void usarFonte(FonteDeConexao novaFonte) {
        fecharGuardadas();
        fonte = novaFonte;
    }

    /** Volta a abrir conexoes com o banco de verdade. */
    public static void usarBanco() {
        usarFonte(BANCO);
    }

    /** Quantas conexoes podem ficar guardadas. Usado pelos testes; 0 desliga o reaproveitamento. */
    static synchronized void definirMaximoDeGuardadas(Integer maximo) {
        maximoDeGuardadas = maximo;
    }

    /** Troca o relogio. Usado pelos testes, para simular a passagem do tempo. */
    static synchronized void usarRelogio(LongSupplier novoRelogio) {
        relogio = novoRelogio;
    }

    private static int maximoDeGuardadas() {
        if (maximoDeGuardadas == null) {
            maximoDeGuardadas = Configuracao.doAmbiente().getDbConexoesGuardadas();
        }
        return maximoDeGuardadas;
    }

    private static boolean vencida(Guardada guardada, long agora) {
        return agora - guardada.criadaEm() >= IDADE_MAXIMA_MS
                || agora - guardada.devolvidaEm() >= OCIOSIDADE_MAXIMA_MS;
    }

    private static boolean valida(Connection conexao) {
        try {
            return conexao.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    private static void fechar(Connection conexao) {
        synchronized (ConnectionFactory.class) {
            CRIADAS_EM.remove(conexao);
        }
        try {
            conexao.close();
        } catch (SQLException e) {
            System.err.println("AVISO: Falha ao fechar conexao com o banco: " + e.getMessage());
        }
    }

    /** Thread de fundo, em modo daemon: nao impede a aplicacao de desligar. */
    private static void iniciarFaxina() {
        if (faxina != null) {
            return;
        }
        faxina = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "faxina-de-conexoes");
            thread.setDaemon(true);
            return thread;
        });
        faxina.scheduleWithFixedDelay(() -> {
            try {
                fecharVencidas();
            } catch (RuntimeException e) {
                System.err.println("AVISO: Falha na faxina de conexoes: " + e.getMessage());
            }
        }, FAXINA_INTERVALO_MS, FAXINA_INTERVALO_MS, TimeUnit.MILLISECONDS);
    }

    private static Connection conectarAoBanco() throws SQLException {
        Configuracao configuracao = Configuracao.doAmbiente();
        return DriverManager.getConnection(
                configuracao.getDbUrl(), configuracao.getDbUsuario(), configuracao.getDbSenha());
    }
}
