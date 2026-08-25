package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.ProdutoAtributoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProdutoAtributoJpaRepository extends JpaRepository<ProdutoAtributoEntity, UUID> {

    List<ProdutoAtributoEntity> findByProdutoIdOrderByChave(UUID produtoId);
}
