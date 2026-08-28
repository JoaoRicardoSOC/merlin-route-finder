package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;
import br.com.jence.backend.domain.repository.AfinidadeDeProduto;
import br.com.jence.backend.domain.repository.FacetaDeProdutos;
import br.com.jence.backend.domain.repository.FiltroDeProdutos;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SecaoDoCatalogo;
import br.com.jence.backend.infrastructure.database.entity.ProdutoAtributoEntity;
import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import br.com.jence.backend.infrastructure.database.factory.ProdutoFactory;
import br.com.jence.backend.infrastructure.database.repository.ProdutoAtributoJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.ProdutoJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProdutoRepositoryAdapter implements ProdutoRepository {

    private final ProdutoJpaRepository jpaRepository;
    private final ProdutoAtributoJpaRepository atributoJpaRepository;
    private final ProdutoFactory produtoFactory;

    @PersistenceContext
    private EntityManager entityManager;

    private static final String DE = """
             from tb_produto p
             join tb_ponto_mapa m on m.id = p.ponto_mapa_id""";

    /*
     * Dobra acento para a letra base, dos dois lados de toda comparacao de texto.
     *
     * <b>TRANSLATE e nao CONVERT.</b> O caminho obvio seria convert(x, 'US7ASCII'), e ele
     * quase funciona: dobra ç para c e as agudas e circunflexas. Mas <b>nao dobra o til</b> -
     * a e o viram '?'. Medido no Oracle da FIAP: "Materiais de construcao" nao acha
     * "Materiais de construção", e til esta em construcao, latao, portao, mao, grao,
     * dimensao, instalacao e iluminacao, ou seja, em boa parte do catalogo. Com TRANSLATE o
     * mesmo par da 100.
     *
     * A funcao impede o uso de indice, e isso nao custa nada aqui: sao 111 linhas, e a busca
     * ja era varredura completa por causa do UTL_MATCH.
     */
    private static final String ACENTOS = "áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ";
    private static final String SEM_ACENTOS = "aaaaeeiooouucAAAAEEIOOOUUC";

    /** Envolve uma expressao SQL para que ela seja comparada sem acento e sem caixa. */
    private static String dobrado(String expressao) {
        return "upper(translate(%s, '%s', '%s'))".formatted(expressao, ACENTOS, SEM_ACENTOS);
    }

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

    // ---------------------------------------------------------------- a busca do catalogo

    /*
     * A unica busca de catalogo do sistema, montada em SQL nativo.
     *
     * Nativa porque UTL_MATCH nao tem equivalente em JPQL, e montada em vez de anotada porque
     * os predicados sao opcionais: uma @Query fixa exigiria uma variante por combinacao de
     * filtros, e cada caracteristica nova dobraria o numero delas.
     *
     * SEGURANCA: nenhum valor vindo do cliente entra na string. O SQL so cresce com trechos
     * constantes, e todo valor vai como parametro nomeado.
     */
    @Override
    @Transactional(readOnly = true)
    public Pagina<Produto> buscar(FiltroDeProdutos filtro, int pagina, int tamanho) {
        String onde = condicoes(filtro);

        // Sem termo nao ha similaridade a ordenar, e chamar UTL_MATCH com nulo nao faria
        // sentido nenhum: nesse caso a ordem e alfabetica, que e o que se espera ao navegar.
        String ordem = filtro.temTermo()
                ? " order by utl_match.jaro_winkler_similarity("
                        + dobrado("p.nome") + ", " + dobrado(":termo") + ") desc, p.nome"
                : " order by p.nome";

        Query consulta = entityManager
                .createNativeQuery("select p.*" + DE + onde + ordem, ProdutoEntity.class);
        Query contagem = entityManager
                .createNativeQuery("select count(*)" + DE + onde);

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

    private String condicoes(FiltroDeProdutos filtro) {
        StringBuilder onde = new StringBuilder(" where 1 = 1");

        if (filtro.temTermo()) {
            /*
             * Os dois lados dobrados: o catalogo tem acento e a maioria das pessoas digita
             * sem, ainda mais no celular. Sem isso, "lampada" nao acha "Lâmpada" - o LIKE nao
             * casa e o Jaro-Winkler da 69 contra o corte de 70, um ponto de diferenca. Ver
             * D-73.
             */
            onde.append(" and (" + dobrado("p.nome") + " like '%' || " + dobrado(":termo") + " || '%'"
                    + " or utl_match.jaro_winkler_similarity("
                    + dobrado("p.nome") + ", " + dobrado(":termo") + ") > 70)");
        }
        if (filtro.secao() != null) {
            // Dobrado tambem: a tela manda de volta o nome que /produtos/secoes devolveu, entao
            // casaria de qualquer jeito - mas um link digitado a mao com "Decoracao" nao pode
            // devolver vazio so por falta de cedilha.
            onde.append(" and " + dobrado("m.corredor") + " = " + dobrado(":secao"));
        }
        if (filtro.apenasDisponiveis()) {
            onde.append(" and p.saldo_estoque > 0");
        }

        /*
         * Um EXISTS por caracteristica escolhida.
         *
         * A semantica sai da forma: valores da mesma chave viram um IN, portanto "ou"; chaves
         * diferentes viram EXISTS separados encadeados por AND, portanto "e". Um unico JOIN
         * com todos os valores num IN daria so o "ou", e marcar Tigre mais Bitola 25 mm
         * devolveria tudo que fosse Tigre OU 25 mm - que nao e o que se espera de um filtro.
         *
         * SEGURANCA: o indice do alias e gerado aqui e a chave vem de um enum. Nenhum texto
         * digitado pelo cliente entra na string.
         */
        for (int indice = 0; indice < filtro.atributos().size(); indice++) {
            onde.append("""
                     and exists (select 1 from tb_produto_atributo a%1$d
                                  where a%1$d.produto_id = p.id
                                    and a%1$d.chave = :chave%1$d
                                    and a%1$d.valor in (:valores%1$d))""".formatted(indice));
        }

        return onde.toString();
    }

    private void vincular(FiltroDeProdutos filtro, Query... consultas) {
        for (Query consulta : consultas) {
            if (filtro.temTermo()) {
                consulta.setParameter("termo", filtro.termo());
            }
            if (filtro.secao() != null) {
                consulta.setParameter("secao", filtro.secao());
            }

            int indice = 0;
            for (Map.Entry<AtributoProduto, List<String>> escolha : filtro.atributos().entrySet()) {
                consulta.setParameter("chave" + indice, escolha.getKey().name());
                consulta.setParameter("valores" + indice, escolha.getValue());
                indice++;
            }
        }
    }

    // ---------------------------------------------------------------- navegacao e facetas

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
    public List<FacetaDeProdutos> calcularFacetas(FiltroDeProdutos filtro) {
        Query consulta = entityManager.createNativeQuery(
                "select a.chave, a.valor, count(*)" + DE
                        + " join tb_produto_atributo a on a.produto_id = p.id"
                        + condicoes(filtro)
                        + " group by a.chave, a.valor");

        vincular(filtro, consulta);

        @SuppressWarnings("unchecked")
        List<Object[]> linhas = consulta.getResultList();

        Map<AtributoProduto, List<FacetaDeProdutos.Valor>> porAtributo =
                new EnumMap<>(AtributoProduto.class);

        for (Object[] linha : linhas) {
            porAtributo.computeIfAbsent(
                            AtributoProduto.valueOf((String) linha[0]), chave -> new ArrayList<>())
                    .add(new FacetaDeProdutos.Valor(
                            (String) linha[1], ((Number) linha[2]).longValue()));
        }

        /*
         * O EnumMap ja itera na ordem de declaracao do enum, que e a ordem de exibicao
         * pretendida: marca primeiro, medidas depois. Dentro de cada filtro, o valor mais
         * comum vem antes - e o que o cliente provavelmente procura.
         */
        return porAtributo.entrySet().stream()
                .map(entrada -> new FacetaDeProdutos(
                        entrada.getKey(),
                        entrada.getKey().getRotulo(),
                        entrada.getValue().stream()
                                .sorted(Comparator
                                        .comparingLong(FacetaDeProdutos.Valor::quantidade).reversed()
                                        .thenComparing(FacetaDeProdutos.Valor::valor))
                                .toList()))
                .toList();
    }

    // ---------------------------------------------------------------- atributos de um produto

    @Override
    @Transactional(readOnly = true)
    public List<ValorDeAtributo> buscarAtributosDe(UUID produtoId) {
        return atributoJpaRepository.findByProdutoIdOrderByChave(produtoId).stream()
                .map(entity -> new ValorDeAtributo(entity.getChave(), entity.getValor()))
                .sorted(Comparator.comparing(ValorDeAtributo::atributo))
                .toList();
    }

    @Override
    @Transactional
    public void salvarAtributos(UUID produtoId, List<ValorDeAtributo> atributos) {
        ProdutoEntity produto = jpaRepository.getReferenceById(produtoId);

        // Substitui em vez de acumular: a carga roda a cada inicializacao, e sem apagar antes
        // a unicidade de (produto, chave) recusaria a segunda execucao.
        atributoJpaRepository.deleteAll(atributoJpaRepository.findByProdutoIdOrderByChave(produtoId));
        atributoJpaRepository.flush();

        atributoJpaRepository.saveAll(atributos.stream()
                .map(atributo -> new ProdutoAtributoEntity(
                        UUID.randomUUID(), produto, atributo.atributo(), atributo.valor()))
                .toList());
    }

    // ---------------------------------------------------------------- ruptura e gravacao

    @Override
    @Transactional(readOnly = true)
    public List<Produto> buscarDisponiveisProximosDe(PontoMapa referencia, UUID excluido,
                                                     AfinidadeDeProduto afinidade,
                                                     double raio, int limite) {
        return jpaRepository.buscarDisponiveisProximosDe(
                        referencia.getCoordenadaX(),
                        referencia.getCoordenadaY(),
                        raio,
                        // O id e gravado como varchar (ver ProdutoEntity); na query nativa a
                        // comparacao precisa ser feita no mesmo tipo.
                        excluido.toString(),
                        afinidade.tipo(),
                        afinidade.marca(),
                        PageRequest.of(0, limite))
                .getContent().stream()
                .map(produtoFactory::paraDominio)
                .toList();
    }

    @Override
    @Transactional
    public Produto salvar(Produto produto) {
        ProdutoEntity salvo = jpaRepository.save(produtoFactory.paraPersistencia(produto));
        return produtoFactory.paraDominio(salvo);
    }
}
