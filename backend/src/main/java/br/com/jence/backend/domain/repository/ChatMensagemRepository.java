package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.ChatMensagem;

import java.util.List;
import java.util.UUID;

public interface ChatMensagemRepository {

    /** Historico da conversa em ordem cronologica, usado tambem como contexto do motor RAG. */
    List<ChatMensagem> buscarHistorico(UUID sessaoId);

    ChatMensagem salvar(ChatMensagem mensagem);
}
