package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.infrastructure.database.entity.PontoMapaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface PontoMapaJpaRepository extends JpaRepository<PontoMapaEntity, UUID> {

    List<PontoMapaEntity> findByTipo(TipoPonto tipo);

    /*
     * Apaga pontos gravados com um tipo que TipoPonto nao tem mais.
     *
     * Consulta nativa de proposito: qualquer leitura via JPA tentaria converter a coluna para
     * o enum e falharia justamente nas linhas que precisamos remover. E o delete acontece pelo
     * nome da coluna, sem passar pelo mapeamento.
     */
    @Modifying
    @Transactional
    @Query(value = "delete from TB_PONTO_MAPA where tipo = :tipo", nativeQuery = true)
    int apagarPorTipoBruto(@Param("tipo") String tipo);
}
