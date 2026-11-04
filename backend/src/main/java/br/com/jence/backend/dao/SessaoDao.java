package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso a tabela {@code tb_sessao}. A placa em que o cliente entrou vem junto, num
 * {@code left join}: ela pode nao existir.
 */
public class SessaoDao {

    private static final String SELECT = "select s.id, s.status, s.criado_em, s.expiracao_ttl, s.escaneado_em, "
            + PontoMapaDao.COLUNAS.formatted("m")
            + " from tb_sessao s left join tb_ponto_mapa m on m.id = s.ponto_escaneado_id";

    public Optional<Sessao> buscarPorId(UUID id) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(SELECT + " where s.id = ?")) {
            stm.setString(1, id.toString());
            try (ResultSet result = stm.executeQuery()) {
                return result.next() ? Optional.of(ler(result)) : Optional.empty();
            }
        }
    }

    /** Sessoes ainda ativas cujo TTL ja venceu antes da referencia. */
    public List<Sessao> buscarExpiradas(LocalDateTime referencia) throws SQLException {
        List<Sessao> sessoes = new ArrayList<>();
        try (PreparedStatement stm = conexao().prepareStatement(
                SELECT + " where s.status = ? and s.expiracao_ttl < ?")) {
            stm.setString(1, StatusSessao.ACTIVE.name());
            stm.setObject(2, referencia);
            stm.setFetchSize(100);
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    sessoes.add(ler(result));
                }
            }
        }
        return sessoes;
    }

    /** Insere a sessao, ou atualiza se o id ja existir. */
    public Sessao salvar(Sessao sessao) throws SQLException {
        String sql = """
                merge into tb_sessao d
                using (select ? id from dual) s
                   on (d.id = s.id)
                 when matched then update set
                      status = ?, criado_em = ?, expiracao_ttl = ?, ponto_escaneado_id = ?, escaneado_em = ?
                 when not matched then insert
                      (id, status, criado_em, expiracao_ttl, ponto_escaneado_id, escaneado_em)
                      values (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stm = conexao().prepareStatement(sql)) {
            stm.setString(1, sessao.getId().toString());
            preencherDados(stm, 2, sessao);
            stm.setString(7, sessao.getId().toString());
            preencherDados(stm, 8, sessao);
            stm.executeUpdate();
        }
        return sessao;
    }

    private void preencherDados(PreparedStatement stm, int inicio, Sessao sessao) throws SQLException {
        stm.setString(inicio, sessao.getStatus().name());
        stm.setObject(inicio + 1, sessao.getCriadoEm());
        stm.setObject(inicio + 2, sessao.getExpiracaoTtl());
        if (sessao.getPontoEscaneado() == null) {
            stm.setNull(inicio + 3, Types.VARCHAR);
        } else {
            stm.setString(inicio + 3, sessao.getPontoEscaneado().getId().toString());
        }
        if (sessao.getEscaneadoEm() == null) {
            stm.setNull(inicio + 4, Types.TIMESTAMP);
        } else {
            stm.setObject(inicio + 4, sessao.getEscaneadoEm());
        }
    }

    private Sessao ler(ResultSet result) throws SQLException {
        return Sessao.reconstituir(
                UUID.fromString(result.getString("id")),
                StatusSessao.valueOf(result.getString("status")),
                result.getObject("criado_em", LocalDateTime.class),
                result.getObject("expiracao_ttl", LocalDateTime.class),
                PontoMapaDao.ler(result, "m"),
                result.getObject("escaneado_em", LocalDateTime.class));
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
