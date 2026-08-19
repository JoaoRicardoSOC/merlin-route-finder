package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProdutoJpaRepository extends JpaRepository<ProdutoEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = "pontoMapa")
    Optional<ProdutoEntity> findById(UUID id);

    @Override
    @EntityGraph(attributePaths = "pontoMapa")
    Page<ProdutoEntity> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "pontoMapa")
    Optional<ProdutoEntity> findBySku(String sku);
}
