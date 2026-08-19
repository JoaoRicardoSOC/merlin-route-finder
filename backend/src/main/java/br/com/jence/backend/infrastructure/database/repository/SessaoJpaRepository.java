package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.infrastructure.database.entity.SessaoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SessaoJpaRepository extends JpaRepository<SessaoEntity, UUID> {

    List<SessaoEntity> findByStatusAndExpiracaoTtlBefore(StatusSessao status, LocalDateTime referencia);
}
