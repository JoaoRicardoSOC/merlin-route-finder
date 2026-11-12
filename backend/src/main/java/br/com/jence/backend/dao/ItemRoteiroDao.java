package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.ItemRoteiro;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso a um item do roteiro isolado, com o produto e o ponto do mapa.
 * <p>
 * Itens novos e removidos sao gravados pelo {@link ListaRoteiroDao}: o item pertence a lista e
 * nao conhece a lista dona. Aqui so se le um item e se grava a coleta de um item que ja existe.
 */
// Provisorio: so enquanto o Spring injeta as dependencias. Sai na fase 10 do plano.
public class ItemRoteiroDao {

    public Optional<ItemRoteiro> buscarPorId(UUID id) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(
                "select i.id i_id, i.coletado_em i_coletado_em, " + ProdutoDao.COLUNAS
                        + " from tb_ch26_item_roteiro i"
                        + " join tb_ch26_produto p on p.id = i.produto_id"
                        + " join tb_ch26_ponto_mapa m on m.id = p.ponto_mapa_id"
                        + " where i.id = ?")) {
            stm.setString(1, id.toString());
            try (ResultSet result = stm.executeQuery()) {
                if (!result.next()) {
                    return Optional.empty();
                }
                return Optional.of(ItemRoteiro.reconstituir(
                        UUID.fromString(result.getString("i_id")),
                        ProdutoDao.ler(result),
                        result.getObject("i_coletado_em", LocalDateTime.class)));
            }
        }
    }

    /**
     * Grava a coleta de um item existente.
     *
     * @throws IllegalArgumentException se o item nao existir: itens novos entram pela lista.
     */
    public ItemRoteiro salvar(ItemRoteiro item) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(
                "update tb_ch26_item_roteiro set coletado = ?, coletado_em = ? where id = ?")) {
            ListaRoteiroDao.preencherColeta(stm, 1, item);
            stm.setString(3, item.getId().toString());
            if (stm.executeUpdate() == 0) {
                throw new IllegalArgumentException(
                        "ItemRoteiro %s nao encontrado. Itens novos devem ser gravados pela ListaRoteiro."
                                .formatted(item.getId()));
            }
        }
        ListaRoteiroDao.registrarColeta(item.getId(), item.getColetadoEm());
        return item;
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
