package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.infrastructure.database.entity.PontoMapaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PontoMapaJpaRepository extends JpaRepository<PontoMapaEntity, UUID> {

    List<PontoMapaEntity> findByTipo(TipoPonto tipo);
}
