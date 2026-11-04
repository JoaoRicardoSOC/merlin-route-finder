package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Remetente;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Acesso a tabela {@code tb_chat_mensagem}: o historico da conversa com o assistente. */
// Provisorio: so enquanto o Spring injeta as dependencias. Sai na fase 10 do plano.
@Component
public class ChatMensagemDao {

    /** As mensagens da sessao, da mais antiga para a mais recente. */
    public List<ChatMensagem> buscarHistorico(UUID sessaoId) throws SQLException {
        List<ChatMensagem> mensagens = new ArrayList<>();
        try (PreparedStatement stm = conexao().prepareStatement("""
                select id, sessao_id, remetente, conteudo, enviado_em
                  from tb_chat_mensagem
                 where sessao_id = ?
                 order by enviado_em
                """)) {
            stm.setString(1, sessaoId.toString());
            stm.setFetchSize(100);
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    mensagens.add(ChatMensagem.reconstituir(
                            UUID.fromString(result.getString("id")),
                            UUID.fromString(result.getString("sessao_id")),
                            Remetente.valueOf(result.getString("remetente")),
                            result.getString("conteudo"),
                            result.getObject("enviado_em", LocalDateTime.class)));
                }
            }
        }
        return mensagens;
    }

    public ChatMensagem salvar(ChatMensagem mensagem) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement("""
                insert into tb_chat_mensagem (id, sessao_id, remetente, conteudo, enviado_em)
                values (?, ?, ?, ?, ?)
                """)) {
            stm.setString(1, mensagem.getId().toString());
            stm.setString(2, mensagem.getSessaoId().toString());
            stm.setString(3, mensagem.getRemetente().name());
            stm.setString(4, mensagem.getConteudo());
            stm.setObject(5, mensagem.getEnviadoEm());
            stm.executeUpdate();
        }
        return mensagem;
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
