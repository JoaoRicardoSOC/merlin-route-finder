package br.com.jence.backend.infrastructure.database.repository;

import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.infrastructure.database.entity.PontoMapaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PontoMapaJpaRepository extends JpaRepository<PontoMapaEntity, UUID> {

    List<PontoMapaEntity> findByTipo(TipoPonto tipo);

    Optional<PontoMapaEntity> findByCodigoCurto(String codigoCurto);

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

    /*
     * Renomeia um corredor no lugar, sem criar ponto novo.
     *
     * A carga casa secao existente pelo nome do corredor, entao mudar o nome na planta faria
     * ela criar um ponto novo e vazio - e os produtos, presos ao ponto antigo pela chave
     * estrangeira, ficariam na secao de nome velho. Atualizar a linha preserva o id, e nada
     * mais precisa se mover. Ver D-70.
     */
    @Modifying
    @Transactional
    @Query(value = "update TB_PONTO_MAPA set corredor = :novo where corredor = :antigo",
            nativeQuery = true)
    int renomearCorredor(@Param("antigo") String antigo, @Param("novo") String novo);

    /*
     * Apaga uma prateleira que ficou com o nome de destino e nenhum produto dentro.
     *
     * Existe porque a renomeacao nao pode ser cega: se alguem rodar a versao com o nome novo
     * na planta antes da versao que renomeia - foi o que aconteceu aqui, com o servidor de
     * desenvolvimento reiniciando sozinho no meio da edicao -, a carga cria a secao nova vazia
     * e o corredor passa a existir duas vezes. Renomear por cima produziria duas linhas com o
     * mesmo nome, uma com todos os produtos e outra com nenhum.
     *
     * Apagar a vazia e seguro: prateleira sem produto nao e referenciada por ninguem, e a
     * carga a recria logo depois se ela realmente fizer parte da planta.
     */
    @Modifying
    @Transactional
    @Query(value = """
            delete from TB_PONTO_MAPA
             where corredor = :corredor
               and tipo = 'PRATELEIRA'
               and id not in (select ponto_mapa_id from TB_PRODUTO where ponto_mapa_id is not null)
            """, nativeQuery = true)
    int apagarPrateleiraVaziaChamada(@Param("corredor") String corredor);
}
