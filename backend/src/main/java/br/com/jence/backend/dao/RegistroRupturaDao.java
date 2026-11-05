package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.RegistroRuptura;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Acesso a tabela {@code tb_registro_ruptura}: os relatos de gondola vazia. */
// Provisorio: so enquanto o Spring injeta as dependencias. Sai na fase 10 do plano.
public class RegistroRupturaDao {

    /**
     * Grava o relato numa transacao propria, e nao na de quem chamou.
     * <p>
     * Quando a ruptura nao rende substituto, o caso de uso registra o relato e so entao lanca a
     * excecao que vira 422 - e o rollback dessa excecao levaria embora exatamente o registro que
     * documenta a falha. O relato de que a gondola estava vazia e um fato independente do
     * desfecho da requisicao. Ver D-38 e D-39.
     */
    public RegistroRuptura salvar(RegistroRuptura registro) throws SQLException {
        try (Transacao propria = Transacao.iniciarNova();
             PreparedStatement stm = conexao().prepareStatement("""
                     insert into tb_registro_ruptura
                            (id, sessao_id, item_roteiro_id, produto_faltante_id, produto_sugerido_id,
                             justificativa, origem, registrado_em)
                     values (?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            stm.setString(1, registro.getId().toString());
            stm.setString(2, registro.getSessaoId().toString());
            stm.setString(3, registro.getItemRoteiroId().toString());
            stm.setString(4, registro.getProdutoFaltanteId().toString());
            if (registro.getProdutoSugeridoId() == null) {
                stm.setNull(5, Types.VARCHAR);
            } else {
                stm.setString(5, registro.getProdutoSugeridoId().toString());
            }
            if (registro.getJustificativa() == null) {
                stm.setNull(6, Types.CLOB);
            } else {
                stm.setString(6, registro.getJustificativa());
            }
            stm.setString(7, registro.getOrigem().name());
            stm.setObject(8, registro.getRegistradoEm());
            stm.executeUpdate();
            propria.confirmar();
        }
        return registro;
    }

    /** Os relatos da sessao, do mais recente para o mais antigo. */
    public List<RegistroRuptura> buscarPorSessao(UUID sessaoId) throws SQLException {
        List<RegistroRuptura> registros = new ArrayList<>();
        try (PreparedStatement stm = conexao().prepareStatement("""
                select id, sessao_id, item_roteiro_id, produto_faltante_id, produto_sugerido_id,
                       justificativa, origem, registrado_em
                  from tb_registro_ruptura
                 where sessao_id = ?
                 order by registrado_em desc
                """)) {
            stm.setString(1, sessaoId.toString());
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    String sugerido = result.getString("produto_sugerido_id");
                    registros.add(RegistroRuptura.reconstituir(
                            UUID.fromString(result.getString("id")),
                            UUID.fromString(result.getString("sessao_id")),
                            UUID.fromString(result.getString("item_roteiro_id")),
                            UUID.fromString(result.getString("produto_faltante_id")),
                            sugerido == null ? null : UUID.fromString(sugerido),
                            result.getString("justificativa"),
                            OrigemSugestao.valueOf(result.getString("origem")),
                            result.getObject("registrado_em", LocalDateTime.class)));
                }
            }
        }
        return registros;
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
