package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    /*
     * Query nativa porque UTL_MATCH e uma funcao do Oracle sem equivalente em JPQL.
     * O LIKE cobre busca parcial ("tint" -> "Tinta") e o JARO_WINKLER cobre erro de
     * digitacao ("tnta" -> "Tinta"), que o LIKE sozinho nao acha. Ordena por similaridade
     * para o resultado mais provavel aparecer primeiro.
     */
    @Query(
            value = """
                    select p.* from tb_produto p
                    where upper(p.nome) like upper('%' || :termo || '%')
                       or utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) > 70
                    order by utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) desc, p.nome
                    """,
            countQuery = """
                    select count(*) from tb_produto p
                    where upper(p.nome) like upper('%' || :termo || '%')
                       or utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) > 70
                    """,
            nativeQuery = true
    )
    Page<ProdutoEntity> buscarPorTermo(@Param("termo") String termo, Pageable pageable);
}
