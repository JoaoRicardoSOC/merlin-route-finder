package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.infrastructure.database.entity.ChatMensagemEntity;
import br.com.jence.backend.infrastructure.database.factory.ChatMensagemFactory;
import br.com.jence.backend.infrastructure.database.repository.ChatMensagemJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ChatMensagemRepositoryAdapter implements ChatMensagemRepository {

    private final ChatMensagemJpaRepository jpaRepository;
    private final ChatMensagemFactory chatMensagemFactory;

    @Override
    @Transactional(readOnly = true)
    public List<ChatMensagem> buscarHistorico(UUID sessaoId) {
        return jpaRepository.findBySessaoIdOrderByEnviadoEmAsc(sessaoId).stream()
                .map(chatMensagemFactory::paraDominio)
                .toList();
    }

    @Override
    @Transactional
    public ChatMensagem salvar(ChatMensagem mensagem) {
        ChatMensagemEntity salva = jpaRepository.save(chatMensagemFactory.paraPersistencia(mensagem));
        return chatMensagemFactory.paraDominio(salva);
    }
}
