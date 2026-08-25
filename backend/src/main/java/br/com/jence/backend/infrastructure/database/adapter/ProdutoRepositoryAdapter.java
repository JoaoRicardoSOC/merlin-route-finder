package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.repository.FiltroDeProdutos;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SecaoDoCatalogo;
import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import br.com.jence.backend.infrastructure.database.factory.ProdutoFactory;
import br.com.jence.backend.infrastructure.database.repository.ProdutoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProdutoRepositoryAdapter implements ProdutoRepository {

    private final ProdutoJpaRepository jpaRepository;
    private final ProdutoFactory produtoFactory;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<Produto> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(produtoFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Produto> buscarPorSku(String sku) {
        return jpaRepository.findBySku(sku).map(produtoFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Produto> buscarPaginado(int pagina, int tamanho) {
        return buscar(FiltroDeProdutos.nenhum(), pagina, tamanho);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho) {
        return buscar(FiltroDeProdutos.porTermo(termo), pagina, tamanho);
    }

    /*
     * A unica busca de catalogo do sistema, montada em SQL nativo.
     *
     * Nativa porque UTL_MATCH nao tem equivalente em JPQL, e montada em vez de anotada porque
     * os predicados sao opcionais: uma @Query fixa exigiria uma variante por combinacao de
     * filtros, e cada faceta nova dobraria o numero delas.
     *
     * SEGURANCA: nenhum valor vindo do cliente entra na string. O SQL so cresce com trechos
     * constantes, e termo e secao vao sempre como parametro nomeado.
     */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Produto> buscar(FiltroDeProdutos filtro, int pagina, int tamanho) {
        StringBuilder onde = new StringBuilder(" where 1 = 1");

        if (filtro.temTermo()) {
            onde.append("""
                     and (upper(p.nome) like upper('%' || :termo || '%')
                          or utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) > 70)""");
        }
        if (filtro.secao() != null) {
            onde.append(" and upper(m.corredor) = upper(:secao)");
        }
        if (filtro.apenasDisponiveis()) {
            onde.append(" and p.saldo_estoque > 0");
        }

        String de = " from tb_produto p join tb_ponto_mapa m on m.id = p.ponto_mapa_id";

        // Sem termo nao ha similaridade a ordenar, e chamar UTL_MATCH com nulo nao faria
        // sentido nenhum: nesse caso a ordem e alfabetica, que e o que se espera ao navegar.
        String ordem = filtro.temTermo()
                ? " order by utl_match.jaro_winkler_similarity(upper(p.nome), upper(:termo)) desc, p.nome"
                : " order by p.nome";

        Query consulta = entityManager
                .createNativeQuery("select p.*" + de + onde + ordem, ProdutoEntity.class);
        Query contagem = entityManager
                .createNativeQuery("select count(*)" + de + onde);

        vincular(filtro, consulta, contagem);

        long total = ((Number) contagem.getSingleResult()).longValue();

        @SuppressWarnings("unchecked")
        List<ProdutoEntity> encontrados = consulta
                .setFirstResult(pagina * tamanho)
                .setMaxResults(tamanho)
                .getResultList();

        return new Pagina<>(
                encontrados.stream().map(produtoFactory::paraDominio).toList(),
                pagina,
                tamanho,
                total,
                (int) Math.ceil((double) total / tamanho));
    }

    private void vincular(FiltroDeProdutos filtro, Query... consultas) {
        for (Query consulta : consultas) {
            if (filtro.temTermo()) {
                consulta.setParameter("termo", filtro.termo());
            }
            if (filtro.secao() != null) {
                consulta.setParameter("secao", filtro.secao());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecaoDoCatalogo> listarSecoes() {
        @SuppressWarnings("unchecked")
        List<Object[]> linhas = entityManager.createNativeQuery("""
                select m.corredor, count(*)
                  from tb_produto p
                  join tb_ponto_mapa m on m.id = p.ponto_mapa_id
                 group by m.corredor
                 order by m.corredor
                """).getResultList();

        return linhas.stream()
                .map(linha -> new SecaoDoCatalogo(
                        (String) linha[0], ((Number) linha[1]).longValue()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Produto> buscarDisponiveisProximosDe(PontoMapa referencia, UUID excluido,
                                                     double raio, int limite) {
        return jpaRepository.buscarDisponiveisProximosDe(
                        referencia.getCoordenadaX(),
                        referencia.getCoordenadaY(),
                        raio,
                        // O id e gravado como varchar (ver ProdutoEntity); na query nativa a
                        // comparacao precisa ser feita no mesmo tipo.
                        excluido.toString(),
                        PageRequest.of(0, limite))
                .getContent().stream()
                .map(produtoFactory::paraDominio)
                .toList();
    }

    private Pagina<Produto> converter(Page<ProdutoEntity> page) {
        return new Pagina<>(
                page.getContent().stream().map(produtoFactory::paraDominio).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public Produto salvar(Produto produto) {
        ProdutoEntity salvo = jpaRepository.save(produtoFactory.paraPersistencia(produto));
        return produtoFactory.paraDominio(salvo);
    }
}
