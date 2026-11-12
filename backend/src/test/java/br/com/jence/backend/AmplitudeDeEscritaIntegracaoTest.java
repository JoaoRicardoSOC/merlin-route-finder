package br.com.jence.backend;

import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.service.RemoverProdutoDoRoteiroUseCase;
import br.com.jence.backend.model.ItemRoteiro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Quantas linhas cada operacao do roteiro escreve no banco.
 * <p>
 * Marcar um item como coletado toca <b>uma</b> linha; adicionar insere uma; remover apaga uma.
 * Nenhuma delas reescreve os irmaos, apesar de os casos de uso gravarem pelo agregado - porque
 * o {@code ListaRoteiroDao} compara com a foto de quando leu e so escreve o que mudou (D-48).
 * <p>
 * O teste mais util aqui e o do roteiro de oito itens: ele fixa que <b>o custo de marcar um
 * item nao cresce com o tamanho da lista</b>, o que importa para o cliente B2B que monta a
 * obra inteira num carrinho sem limite (D-17).
 * <p>
 * Serve tambem de alarme: se alguem um dia trocar a gravacao por um caminho que remonte o
 * agregado fora da transacao de leitura, estes numeros sobem e o teste avisa.
 * <p>
 * A contagem e feita na propria conexao JDBC: durante o teste, a {@link ConnectionFactory}
 * entrega uma conexao de verdade embrulhada num {@link Proxy}, que embrulha cada
 * {@link PreparedStatement} e soma as linhas que cada {@code insert}, {@code update} e
 * {@code delete} em {@code tb_ch26_item_roteiro} diz ter afetado.
 */
@Tag("integracao")
class AmplitudeDeEscritaIntegracaoTest {

    /** A massa de demonstracao no banco, como a Main garante antes de subir. */
    @BeforeAll
    static void carregarMassa() throws Exception {
        BancoDeTeste.carregarMassa();
    }

    private final InicializarSessaoUseCase inicializar = new InicializarSessaoUseCase(
            new SessaoDao(),
            new ListaRoteiroDao(),
            new PontoMapaDao());
    private final AdicionarProdutoAoRoteiroUseCase adicionar = new AdicionarProdutoAoRoteiroUseCase(
            new SessaoDao(),
            new ListaRoteiroDao(),
            new ProdutoDao());
    private final MarcarItemColetadoUseCase marcarColetado =
            new MarcarItemColetadoUseCase(new ListaRoteiroDao(), new SessaoDao());
    private final RemoverProdutoDoRoteiroUseCase remover =
            new RemoverProdutoDoRoteiroUseCase(new SessaoDao(), new ListaRoteiroDao());
    private final ListaRoteiroDao listaRoteiroDao = BancoDeTeste.listas();
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();

    private UUID sessaoId;

    /** Linhas de {@code tb_ch26_item_roteiro} escritas desde o inicio do teste, por tipo de comando. */
    private final AtomicLong atualizadas = new AtomicLong();
    private final AtomicLong inseridas = new AtomicLong();
    private final AtomicLong apagadas = new AtomicLong();

    @BeforeEach
    void montarRoteiro() throws Exception {
        ConnectionFactory.usarFonte(this::conexaoQueConta);

        sessaoId = inicializar.executar(null).getId();
        adicionarAoRoteiro("SKU-TIN-001");
        adicionarAoRoteiro("SKU-ENC-001");
        adicionarAoRoteiro("SKU-JAR-001");
    }

    @AfterEach
    void limpar() throws Exception {
        ConnectionFactory.usarBanco();
        BancoDeTeste.apagarSessao(sessaoId);
    }

    private void adicionarAoRoteiro(String sku) throws Exception {
        adicionar.executar(sessaoId, produtoDao.buscarPorSku(sku).orElseThrow().getId());
    }

    private List<ItemRoteiro> itens() throws Exception {
        return listaRoteiroDao.buscarPorSessao(sessaoId).orElseThrow().getItens().stream()
                .sorted(Comparator.comparing(i -> i.getProduto().getSku()))
                .toList();
    }

    /** Linhas de item gravadas, inseridas ou apagadas por uma operacao. */
    private record Escrita(long atualizadas, long inseridas, long apagadas) {

        long total() {
            return atualizadas + inseridas + apagadas;
        }

        @Override
        public String toString() {
            return "%d update(s), %d insert(s), %d delete(s)".formatted(atualizadas, inseridas, apagadas);
        }
    }

    /** Uma operacao que pode lancar excecao checada, como os casos de uso que ja usam JDBC. */
    private interface Operacao {
        void executar() throws Exception;
    }

    private Escrita medir(Operacao operacao) throws Exception {
        long u = atualizadas.get();
        long i = inseridas.get();
        long d = apagadas.get();

        operacao.executar();

        return new Escrita(atualizadas.get() - u,
                inseridas.get() - i,
                apagadas.get() - d);
    }

    // ---------------------------------------------------------------- contagem na conexao

    /** Uma conexao de verdade com o banco, que entrega comandos que se contam. */
    private Connection conexaoQueConta() throws SQLException {
        Connection real = DriverManager.getConnection(
                System.getenv("DB_URL"), System.getenv("DB_USER"), System.getenv("DB_PASSWORD"));
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                (proxy, metodo, argumentos) -> {
                    Object resultado = repassar(real, metodo, argumentos);
                    if (metodo.getName().equals("prepareStatement")
                            && resultado instanceof PreparedStatement comando) {
                        return queConta(comando, (String) argumentos[0]);
                    }
                    return resultado;
                });
    }

    /** O comando, que ao executar soma as linhas afetadas no contador do seu tipo. */
    private PreparedStatement queConta(PreparedStatement real, String sql) {
        String texto = sql.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
        AtomicLong contador = texto.startsWith("insert into tb_ch26_item_roteiro") ? inseridas
                : texto.startsWith("update tb_ch26_item_roteiro") ? atualizadas
                : texto.startsWith("delete from tb_ch26_item_roteiro") ? apagadas
                : null;

        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                (proxy, metodo, argumentos) -> {
                    Object resultado = repassar(real, metodo, argumentos);
                    if (contador != null && metodo.getName().equals("executeUpdate")
                            && (argumentos == null || argumentos.length == 0)) {
                        contador.addAndGet((Integer) resultado);
                    }
                    return resultado;
                });
    }

    /** Chama o metodo no objeto de verdade, devolvendo a excecao original e nao a da reflexao. */
    private static Object repassar(Object alvo, Method metodo, Object[] argumentos) throws Throwable {
        try {
            return metodo.invoke(alvo, argumentos);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @Test
    @DisplayName("marcar um item toca a linha daquele item, e so ela")
    void marcarTocaUmaLinha() throws Exception {
        UUID alvo = itens().getFirst().getId();

        Escrita escrita = medir(() -> marcarColetado.executar(alvo));

        System.out.println(">>> marcar item coletado (roteiro de 3): " + escrita);

        assertEquals(1L, escrita.atualizadas(),
                "so a linha do item marcado deveria mudar");
        assertEquals(0L, escrita.apagadas(),
                "marcar como coletado nunca deveria apagar item nenhum");
    }

    @Test
    @DisplayName("adicionar um produto insere uma linha e nao reescreve as existentes")
    void adicionarInsereUmaLinha() throws Exception {
        UUID furadeira = produtoDao.buscarPorSku("SKU-FER-001").orElseThrow().getId();

        Escrita escrita = medir(() -> adicionar.executar(sessaoId, furadeira));

        System.out.println(">>> adicionar produto (roteiro de 3): " + escrita);

        assertEquals(1L, escrita.inseridas());
        assertEquals(0L, escrita.atualizadas(),
                "os itens que ja estavam la nao mudaram");
        assertEquals(0L, escrita.apagadas());
    }

    @Test
    @DisplayName("remover um item apaga uma linha e nao reescreve as outras")
    void removerApagaUmaLinha() throws Exception {
        UUID alvo = itens().getLast().getId();

        Escrita escrita = medir(() -> remover.executar(sessaoId, alvo));

        System.out.println(">>> remover item (roteiro de 3): " + escrita);

        assertEquals(1L, escrita.apagadas());
        assertEquals(0L, escrita.atualizadas(),
                "remover um item nao pode reescrever o estado dos outros");
    }

    @Test
    @DisplayName("a amplitude nao cresce com o tamanho do roteiro")
    void amplitudeNaoCresceComORoteiro() throws Exception {
        /*
         * Oito itens e o tamanho de uma lista de obra pequena. Se o custo de marcar um item
         * dependesse do tamanho do roteiro, o cliente B2B - que a D-17 deixou sem limite -
         * pagaria cada vez mais caro conforme a lista crescesse. Nao paga: continua sendo uma
         * linha, porque o DAO so escreve o que mudou.
         */
        for (String sku : List.of("SKU-FER-001", "SKU-ILU-001", "SKU-MAT-001",
                "SKU-COZ-001", "SKU-DEC-001")) {
            adicionarAoRoteiro(sku);
        }

        assertEquals(8, itens().size());
        UUID alvo = itens().getFirst().getId();

        Escrita escrita = medir(() -> marcarColetado.executar(alvo));

        System.out.println(">>> marcar item coletado (roteiro de 8): " + escrita);

        assertEquals(1L, escrita.total(),
                "o custo de marcar um item nao pode depender de quantos itens o cliente tem");
    }
}
