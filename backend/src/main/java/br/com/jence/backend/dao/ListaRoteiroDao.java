package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Acesso ao roteiro: {@code tb_lista_roteiro} e seus itens em {@code tb_item_roteiro}.
 *
 * <h2>Grava so o que mudou desde a leitura (D-48)</h2>
 * Os casos de uso leem a lista, mexem nela e a gravam inteira. Se a gravacao comparasse com o
 * que esta no banco <i>agora</i>, um item adicionado por outra requisicao entre a leitura e a
 * gravacao pareceria "removido" e seria apagado - foi para impedir isso que o projeto passou a
 * ler e gravar na mesma transacao, e o {@code EscritaObsoletaIntegracaoTest} demonstra.
 * <p>
 * O Hibernate resolvia isso comparando com o estado de quando carregou. Este DAO faz o mesmo: ao
 * ler, guarda na {@link Transacao} uma foto de cada item (se estava coletado e quando), e ao
 * gravar compara a lista com essa foto. Item que nao estava na foto e inserido; item da foto que
 * saiu da lista e apagado; item que mudou e atualizado; o resto nao e tocado. Item que outra
 * requisicao incluiu, e que esta leitura nunca viu, fica fora da conta - e por isso sobrevive.
 * <p>
 * Lista gravada sem ter sido lida nesta transacao (a lista nova de uma sessao, por exemplo) e
 * comparada com o que o banco tem, como o Hibernate fazia nesse caso.
 */
// Provisorio: so enquanto o Spring injeta as dependencias. Sai na fase 10 do plano.
public class ListaRoteiroDao {

    /** Chave da foto nos recursos da transacao. */
    private static final Object FOTO = ListaRoteiroDao.class;

    private static final String SELECT = "select l.id l_id, l.sessao_id l_sessao_id, "
            + "i.id i_id, i.coletado_em i_coletado_em, " + ProdutoDao.COLUNAS
            + " from tb_lista_roteiro l"
            + " left join tb_item_roteiro i on i.lista_id = l.id"
            + " left join tb_produto p on p.id = i.produto_id"
            + " left join tb_ponto_mapa m on m.id = p.ponto_mapa_id";

    public Optional<ListaRoteiro> buscarPorId(UUID id) throws SQLException {
        return buscarUma(" where l.id = ?", id);
    }

    public Optional<ListaRoteiro> buscarPorSessao(UUID sessaoId) throws SQLException {
        return buscarUma(" where l.sessao_id = ?", sessaoId);
    }

    /** A lista que contem o item. */
    public Optional<ListaRoteiro> buscarPorItem(UUID itemId) throws SQLException {
        return buscarUma(" where l.id = (select lista_id from tb_item_roteiro where id = ?)", itemId);
    }

    public ListaRoteiro salvar(ListaRoteiro lista) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement("""
                merge into tb_lista_roteiro d
                using (select ? id, ? sessao_id from dual) s
                   on (d.id = s.id)
                 when not matched then insert (id, sessao_id) values (s.id, s.sessao_id)
                """)) {
            stm.setString(1, lista.getId().toString());
            stm.setString(2, lista.getSessaoId().toString());
            stm.executeUpdate();
        }

        Map<UUID, LocalDateTime> lidos = foto().get(lista.getId());
        if (lidos == null) {
            lidos = itensNoBanco(lista.getId());
        }

        Set<UUID> naLista = new HashSet<>();
        for (ItemRoteiro item : lista.getItens()) {
            naLista.add(item.getId());
            if (!lidos.containsKey(item.getId())) {
                inserir(lista.getId(), item);
            } else if (!Objects.equals(lidos.get(item.getId()), item.getColetadoEm())) {
                atualizarColeta(item);
            }
        }
        for (UUID lido : lidos.keySet()) {
            if (!naLista.contains(lido)) {
                apagar(lido);
            }
        }

        // A proxima gravacao nesta transacao parte do que acabou de ser gravado.
        Map<UUID, LocalDateTime> novaFoto = new HashMap<>();
        for (ItemRoteiro item : lista.getItens()) {
            novaFoto.put(item.getId(), item.getColetadoEm());
        }
        foto().put(lista.getId(), novaFoto);
        return lista;
    }

    /**
     * Atualiza a foto de um item gravado por fora da lista, pelo {@link ItemRoteiroDao}, para que
     * uma gravacao posterior da lista nao o reescreva a toa.
     */
    static void registrarColeta(UUID itemId, LocalDateTime coletadoEm) {
        for (Map<UUID, LocalDateTime> itens : foto().values()) {
            if (itens.containsKey(itemId)) {
                itens.put(itemId, coletadoEm);
            }
        }
    }

    private Optional<ListaRoteiro> buscarUma(String condicao, UUID valor) throws SQLException {
        UUID listaId = null;
        UUID sessaoId = null;
        List<ItemRoteiro> itens = new ArrayList<>();

        try (PreparedStatement stm = conexao().prepareStatement(SELECT + condicao)) {
            stm.setString(1, valor.toString());
            stm.setFetchSize(100);
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    UUID desta = UUID.fromString(result.getString("l_id"));
                    if (listaId == null) {
                        listaId = desta;
                        sessaoId = UUID.fromString(result.getString("l_sessao_id"));
                    } else if (!listaId.equals(desta)) {
                        continue;
                    }
                    String itemId = result.getString("i_id");
                    if (itemId != null) {
                        itens.add(ItemRoteiro.reconstituir(
                                UUID.fromString(itemId),
                                ProdutoDao.ler(result),
                                result.getObject("i_coletado_em", LocalDateTime.class)));
                    }
                }
            }
        }

        if (listaId == null) {
            return Optional.empty();
        }

        Map<UUID, LocalDateTime> lidos = new HashMap<>();
        for (ItemRoteiro item : itens) {
            lidos.put(item.getId(), item.getColetadoEm());
        }
        foto().put(listaId, lidos);

        return Optional.of(ListaRoteiro.reconstituir(listaId, sessaoId, itens));
    }

    private Map<UUID, LocalDateTime> itensNoBanco(UUID listaId) throws SQLException {
        Map<UUID, LocalDateTime> itens = new HashMap<>();
        try (PreparedStatement stm = conexao().prepareStatement(
                "select id, coletado_em from tb_item_roteiro where lista_id = ?")) {
            stm.setString(1, listaId.toString());
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    itens.put(UUID.fromString(result.getString("id")),
                            result.getObject("coletado_em", LocalDateTime.class));
                }
            }
        }
        return itens;
    }

    private void inserir(UUID listaId, ItemRoteiro item) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement("""
                insert into tb_item_roteiro (id, lista_id, produto_id, coletado, coletado_em)
                values (?, ?, ?, ?, ?)
                """)) {
            stm.setString(1, item.getId().toString());
            stm.setString(2, listaId.toString());
            stm.setString(3, item.getProduto().getId().toString());
            preencherColeta(stm, 4, item);
            stm.executeUpdate();
        }
    }

    private void atualizarColeta(ItemRoteiro item) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(
                "update tb_item_roteiro set coletado = ?, coletado_em = ? where id = ?")) {
            preencherColeta(stm, 1, item);
            stm.setString(3, item.getId().toString());
            stm.executeUpdate();
        }
    }

    private void apagar(UUID itemId) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement("delete from tb_item_roteiro where id = ?")) {
            stm.setString(1, itemId.toString());
            stm.executeUpdate();
        }
    }

    /** {@code coletado} (0 ou 1) e {@code coletado_em}, nas duas posicoes a partir de {@code inicio}. */
    static void preencherColeta(PreparedStatement stm, int inicio, ItemRoteiro item) throws SQLException {
        stm.setInt(inicio, item.isColetado() ? 1 : 0);
        if (item.getColetadoEm() == null) {
            stm.setNull(inicio + 1, Types.TIMESTAMP);
        } else {
            stm.setObject(inicio + 1, item.getColetadoEm());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<UUID, Map<UUID, LocalDateTime>> foto() {
        return (Map<UUID, Map<UUID, LocalDateTime>>) Transacao.emAndamento().getRecursos()
                .computeIfAbsent(FOTO, chave -> new HashMap<UUID, Map<UUID, LocalDateTime>>());
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
