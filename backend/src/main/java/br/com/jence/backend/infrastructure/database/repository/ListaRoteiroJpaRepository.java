package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.ListaRoteiroEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ListaRoteiroJpaRepository extends JpaRepository<ListaRoteiroEntity, UUID> {

    @Override
    @EntityGraph(attributePaths = {"itens", "itens.produto", "itens.produto.pontoMapa"})
    Optional<ListaRoteiroEntity> findById(UUID id);

    @EntityGraph(attributePaths = {"itens", "itens.produto", "itens.produto.pontoMapa"})
    Optional<ListaRoteiroEntity> findBySessaoId(UUID sessaoId);

    @EntityGraph(attributePaths = {"itens", "itens.produto", "itens.produto.pontoMapa"})
    Optional<ListaRoteiroEntity> findByItensId(UUID itemId);
}
