package br.com.jence.backend;

import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.RegistroRupturaDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.AfinidadeDeProduto;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.FacetaDeProdutos;
import br.com.jence.backend.model.FiltroDeProdutos;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.Pagina;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.RegistroRuptura;
import br.com.jence.backend.model.SecaoDoCatalogo;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.model.ValorDeAtributo;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso ao banco para os testes de integracao montarem e conferirem dados.
 * <p>
 * Os DAOs de verdade so funcionam dentro de uma {@link Transacao}, aberta pelos services. Nos
 * testes, quem le e grava e o proprio teste, entre uma chamada de service e outra. Por isso aqui
 * cada metodo abre e confirma a sua transacao - como os repositorios do Spring faziam.
 */
public final class BancoDeTeste {

    private BancoDeTeste() {
    }

    public static ProdutoDao produtos() {
        return new ProdutosEmTransacao();
    }

    public static PontoMapaDao pontos() {
        return new PontosEmTransacao();
    }

    public static SessaoDao sessoes() {
        return new SessoesEmTransacao();
    }

    public static ListaRoteiroDao listas() {
        return new ListasEmTransacao();
    }

    public static RegistroRupturaDao rupturas() {
        return new RupturasEmTransacao();
    }

    public static ChatMensagemDao mensagens() {
        return new MensagensEmTransacao();
    }

    /**
     * Apaga tudo o que um teste criou para uma sessao: mensagens do chat, relatos de ruptura,
     * itens e lista do roteiro e a propria sessao. O schema e o mesmo da instancia publicada, entao
     * nenhum teste pode deixar residuo.
     */
    public static void apagarSessao(UUID sessaoId) throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            executar("delete from tb_chat_mensagem where sessao_id = ?", sessaoId);
            executar("delete from tb_registro_ruptura where sessao_id = ?", sessaoId);
            executar("delete from tb_item_roteiro where lista_id in "
                    + "(select id from tb_lista_roteiro where sessao_id = ?)", sessaoId);
            executar("delete from tb_lista_roteiro where sessao_id = ?", sessaoId);
            executar("delete from tb_sessao where id = ?", sessaoId);
            transacao.confirmar();
        }
    }

    private static void executar(String sql, UUID id) throws SQLException {
        try (PreparedStatement stm = ConnectionFactory.getConnection().prepareStatement(sql)) {
            stm.setString(1, id.toString());
            stm.executeUpdate();
        }
    }

    /** Uma chamada a um DAO, para rodar dentro de uma transacao. */
    @FunctionalInterface
    private interface Chamada<T> {
        T executar() throws SQLException;
    }

    private static <T> T emTransacao(Chamada<T> chamada) throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            T resultado = chamada.executar();
            transacao.confirmar();
            return resultado;
        }
    }

    // ---------------------------------------------------------------- DAOs com transacao propria

    private static final class ProdutosEmTransacao extends ProdutoDao {
        @Override
        public Optional<Produto> buscarPorId(UUID id) throws SQLException {
            return emTransacao(() -> super.buscarPorId(id));
        }

        @Override
        public Optional<Produto> buscarPorSku(String sku) throws SQLException {
            return emTransacao(() -> super.buscarPorSku(sku));
        }

        @Override
        public Pagina<Produto> buscarPaginado(int pagina, int tamanho) throws SQLException {
            return emTransacao(() -> super.buscarPaginado(pagina, tamanho));
        }

        @Override
        public Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho) throws SQLException {
            return emTransacao(() -> super.buscarPorTermo(termo, pagina, tamanho));
        }

        @Override
        public Pagina<Produto> buscar(FiltroDeProdutos filtro, int pagina, int tamanho) throws SQLException {
            return emTransacao(() -> super.buscar(filtro, pagina, tamanho));
        }

        @Override
        public List<SecaoDoCatalogo> listarSecoes() throws SQLException {
            return emTransacao(super::listarSecoes);
        }

        @Override
        public List<FacetaDeProdutos> calcularFacetas(FiltroDeProdutos filtro) throws SQLException {
            return emTransacao(() -> super.calcularFacetas(filtro));
        }

        @Override
        public List<ValorDeAtributo> buscarAtributosDe(UUID produtoId) throws SQLException {
            return emTransacao(() -> super.buscarAtributosDe(produtoId));
        }

        @Override
        public void salvarAtributos(UUID produtoId, List<ValorDeAtributo> atributos) throws SQLException {
            emTransacao(() -> {
                super.salvarAtributos(produtoId, atributos);
                return null;
            });
        }

        @Override
        public List<Produto> buscarDisponiveisProximosDe(PontoMapa referencia, UUID excluido,
                                                         AfinidadeDeProduto afinidade,
                                                         double raio, int limite) throws SQLException {
            return emTransacao(() -> super.buscarDisponiveisProximosDe(referencia, excluido, afinidade, raio, limite));
        }

        @Override
        public Produto salvar(Produto produto) throws SQLException {
            return emTransacao(() -> super.salvar(produto));
        }
    }

    private static final class PontosEmTransacao extends PontoMapaDao {
        @Override
        public Optional<PontoMapa> buscarPorId(UUID id) throws SQLException {
            return emTransacao(() -> super.buscarPorId(id));
        }

        @Override
        public List<PontoMapa> buscarPorTipo(TipoPonto tipo) throws SQLException {
            return emTransacao(() -> super.buscarPorTipo(tipo));
        }

        @Override
        public Optional<PontoMapa> buscarPorCodigoCurto(String codigoCurto) throws SQLException {
            return emTransacao(() -> super.buscarPorCodigoCurto(codigoCurto));
        }

        @Override
        public PontoMapa salvar(PontoMapa ponto) throws SQLException {
            return emTransacao(() -> super.salvar(ponto));
        }

        @Override
        public int renomearCorredor(String antigo, String novo) throws SQLException {
            return emTransacao(() -> super.renomearCorredor(antigo, novo));
        }
    }

    private static final class SessoesEmTransacao extends SessaoDao {
        @Override
        public Optional<Sessao> buscarPorId(UUID id) throws SQLException {
            return emTransacao(() -> super.buscarPorId(id));
        }

        @Override
        public List<Sessao> buscarExpiradas(LocalDateTime referencia) throws SQLException {
            return emTransacao(() -> super.buscarExpiradas(referencia));
        }

        @Override
        public Sessao salvar(Sessao sessao) throws SQLException {
            return emTransacao(() -> super.salvar(sessao));
        }
    }

    private static final class ListasEmTransacao extends ListaRoteiroDao {
        @Override
        public Optional<ListaRoteiro> buscarPorId(UUID id) throws SQLException {
            return emTransacao(() -> super.buscarPorId(id));
        }

        @Override
        public Optional<ListaRoteiro> buscarPorSessao(UUID sessaoId) throws SQLException {
            return emTransacao(() -> super.buscarPorSessao(sessaoId));
        }

        @Override
        public Optional<ListaRoteiro> buscarPorItem(UUID itemId) throws SQLException {
            return emTransacao(() -> super.buscarPorItem(itemId));
        }

        @Override
        public ListaRoteiro salvar(ListaRoteiro lista) throws SQLException {
            return emTransacao(() -> super.salvar(lista));
        }
    }

    private static final class RupturasEmTransacao extends RegistroRupturaDao {
        @Override
        public List<RegistroRuptura> buscarPorSessao(UUID sessaoId) throws SQLException {
            return emTransacao(() -> super.buscarPorSessao(sessaoId));
        }

        @Override
        public RegistroRuptura salvar(RegistroRuptura registro) throws SQLException {
            return emTransacao(() -> super.salvar(registro));
        }
    }

    private static final class MensagensEmTransacao extends ChatMensagemDao {
        @Override
        public List<ChatMensagem> buscarHistorico(UUID sessaoId) throws SQLException {
            return emTransacao(() -> super.buscarHistorico(sessaoId));
        }

        @Override
        public ChatMensagem salvar(ChatMensagem mensagem) throws SQLException {
            return emTransacao(() -> super.salvar(mensagem));
        }
    }
}
