package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.ItemRoteiroEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ItemRoteiroJpaRepository extends JpaRepository<ItemRoteiroEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"produto", "produto.pontoMapa"})
    Optional<ItemRoteiroEntity> findById(UUID id);
}
