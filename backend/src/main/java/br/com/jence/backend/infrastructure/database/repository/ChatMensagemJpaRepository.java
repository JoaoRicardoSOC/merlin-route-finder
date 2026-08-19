package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.ChatMensagemEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMensagemJpaRepository extends JpaRepository<ChatMensagemEntity, UUID> {

    List<ChatMensagemEntity> findBySessaoIdOrderByEnviadoEmAsc(UUID sessaoId);
}
