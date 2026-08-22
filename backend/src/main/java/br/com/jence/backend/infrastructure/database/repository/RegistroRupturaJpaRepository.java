package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.RegistroRupturaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface RegistroRupturaJpaRepository extends JpaRepository<RegistroRupturaEntity, UUID> {

    List<RegistroRupturaEntity> findBySessaoIdOrderByRegistradoEmDesc(UUID sessaoId);
}
