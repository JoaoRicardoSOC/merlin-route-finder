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

    /*
     * Pre-filtragem espacial da ruptura de estoque (UC-013). Distancia euclidiana no grid da
     * loja, calculada no banco: nativa pelo mesmo motivo da consulta acima - JPQL nao tem
     * sqrt/power, e ordenar por distancia em memoria exigiria carregar o catalogo inteiro.
     *
     * O produto em falta e excluido explicitamente. Ele esta a distancia zero de si mesmo e,
     * como o saldo do sistema pode nao refletir a prateleira vazia, apareceria como o
     * "melhor" candidato a substituir a si proprio.
     *
     * A ordenacao poe AFINIDADE antes de distancia: mesmo tipo primeiro, depois mesma marca, e
     * so entao proximidade. Sem isso, produtos da mesma secao empatam em distancia e o
     * desempate vira o nome - o que fez o fallback sugerir bandeja de pintura para uma lixa
     * assim que o catalogo cresceu. Ver D-68.
     *
     * Tipo e marca nulos nao quebram nada: a comparacao com nulo nunca e verdadeira, os dois
     * CASE devolvem 1 para todos, e a ordem cai de volta para distancia e nome.
     */
    @Query(
            value = """
                    select p.* from tb_produto p
                    join tb_ponto_mapa m on m.id = p.ponto_mapa_id
                    where p.saldo_estoque > 0
                      and p.id <> :excluido
                      and sqrt(power(m.coordenada_x - :x, 2) + power(m.coordenada_y - :y, 2)) <= :raio
                    order by
                      case when exists (select 1 from tb_produto_atributo a
                                         where a.produto_id = p.id
                                           and a.chave = 'TIPO' and a.valor = :tipo) then 0 else 1 end,
                      case when exists (select 1 from tb_produto_atributo a
                                         where a.produto_id = p.id
                                           and a.chave = 'MARCA' and a.valor = :marca) then 0 else 1 end,
                      sqrt(power(m.coordenada_x - :x, 2) + power(m.coordenada_y - :y, 2)),
                      p.nome
                    """,
            countQuery = """
                    select count(*) from tb_produto p
                    join tb_ponto_mapa m on m.id = p.ponto_mapa_id
                    where p.saldo_estoque > 0
                      and p.id <> :excluido
                      and sqrt(power(m.coordenada_x - :x, 2) + power(m.coordenada_y - :y, 2)) <= :raio
                    """,
            nativeQuery = true
    )
    Page<ProdutoEntity> buscarDisponiveisProximosDe(@Param("x") int x,
                                                    @Param("y") int y,
                                                    @Param("raio") double raio,
                                                    @Param("excluido") String excluido,
                                                    @Param("tipo") String tipo,
                                                    @Param("marca") String marca,
                                                    Pageable pageable);
}
