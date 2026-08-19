package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.infrastructure.database.entity.ChatMensagemEntity;
import org.springframework.stereotype.Component;

@Component
public class ChatMensagemFactory {

    public ChatMensagem paraDominio(ChatMensagemEntity entity) {
        if (entity == null) {
            return null;
        }
        return ChatMensagem.reconstituir(
                entity.getId(),
                entity.getSessaoId(),
                entity.getRemetente(),
                entity.getConteudo(),
                entity.getEnviadoEm()
        );
    }

    public ChatMensagemEntity paraPersistencia(ChatMensagem mensagem) {
        if (mensagem == null) {
            return null;
        }
        ChatMensagemEntity entity = new ChatMensagemEntity();
        entity.setId(mensagem.getId());
        entity.setSessaoId(mensagem.getSessaoId());
        entity.setRemetente(mensagem.getRemetente());
        entity.setConteudo(mensagem.getConteudo());
        entity.setEnviadoEm(mensagem.getEnviadoEm());
        return entity;
    }
}
