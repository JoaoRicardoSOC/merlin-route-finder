package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.AfinidadeDeProduto;
import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.FacetaDeProdutos;
import br.com.jence.backend.model.FiltroDeProdutos;
import br.com.jence.backend.model.Pagina;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.SecaoDoCatalogo;
import br.com.jence.backend.model.ValorDeAtributo;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso ao catalogo: {@code tb_produto} e as caracteristicas em {@code tb_produto_atributo}.
 * <p>
 * Usa a conexao da transacao em andamento e nunca a fecha. Todo valor vindo do cliente vai como
 * parametro ({@code ?}); o SQL so cresce com trechos constantes.
 */
public class ProdutoDao {

    /** Colunas do produto com o prefixo {@code p}, mais as do ponto com o prefixo {@code m}. */
    static final String COLUNAS = "p.id p_id, p.sku p_sku, p.nome p_nome, p.descricao p_descricao, "
            + "p.imagem_url p_imagem_url, p.preco p_preco, p.saldo_estoque p_saldo_estoque, "
            + PontoMapaDao.COLUNAS.formatted("m");

    /* Comeca com espaco porque e concatenado logo depois da lista de colunas. */
    private static final String DE = " from tb_produto p join tb_ponto_mapa m on m.id = p.ponto_mapa_id";

    /*
     * Dobra acento para a letra base, dos dois lados de toda comparacao de texto.
     *
     * TRANSLATE e nao CONVERT: convert(x, 'US7ASCII') dobra c-cedilha e as agudas, mas nao o
     * til - "a" e "o" com til viram '?'. Medido no Oracle da FIAP: "Materiais de construcao" nao
     * acha "Materiais de construção", e til esta em boa parte do catalogo. Com TRANSLATE o mesmo
     * par da 100.
     *
     * A funcao impede o uso de indice, e isso nao custa nada aqui: o catalogo e pequeno e a
     * busca ja era varredura completa por causa do UTL_MATCH.
     */
    private static final String ACENTOS = "áàâãéêíóôõúüçÁÀÂÃÉÊÍÓÔÕÚÜÇ";
    private static final String SEM_ACENTOS = "aaaaeeiooouucAAAAEEIOOOUUC";

    /** Envolve uma expressao SQL para que ela seja comparada sem acento e sem caixa. */
    private static String dobrado(String expressao) {
        return "upper(translate(%s, '%s', '%s'))".formatted(expressao, ACENTOS, SEM_ACENTOS);
    }

    public Optional<Produto> buscarPorId(UUID id) throws SQLException {
        return buscarUm("p.id = ?", id.toString());
    }

    public Optional<Produto> buscarPorSku(String sku) throws SQLException {
        return buscarUm("p.sku = ?", sku);
    }

    public Pagina<Produto> buscarPaginado(int pagina, int tamanho) throws SQLException {
        return buscar(FiltroDeProdutos.nenhum(), pagina, tamanho);
    }

    public Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho) throws SQLException {
        return buscar(FiltroDeProdutos.porTermo(termo), pagina, tamanho);
    }

    // ---------------------------------------------------------------- a busca do catalogo

    /**
     * A unica busca de catalogo do sistema.
     * <p>
     * Montada em vez de fixa porque os filtros sao opcionais: um SQL fixo exigiria uma variante
     * por combinacao, e cada caracteristica nova dobraria o numero delas.
     */
    public Pagina<Produto> buscar(FiltroDeProdutos filtro, int pagina, int tamanho) throws SQLException {
        List<Object> parametros = new ArrayList<>();
        String onde = condicoes(filtro, parametros);

        long total;
        try (PreparedStatement stm = conexao().prepareStatement("select count(*)" + DE + onde)) {
            vincular(stm, parametros);
            try (ResultSet result = stm.executeQuery()) {
                result.next();
                total = result.getLong(1);
            }
        }

        // Sem termo nao ha similaridade a ordenar: a ordem e alfabetica, que e o que se espera
        // ao navegar.
        List<Object> parametrosDaConsulta = new ArrayList<>(parametros);
        String ordem;
        if (filtro.temTermo()) {
            ordem = " order by utl_match.jaro_winkler_similarity("
                    + dobrado("p.nome") + ", " + dobrado("?") + ") desc, p.nome";
            parametrosDaConsulta.add(filtro.termo());
        } else {
            ordem = " order by p.nome";
        }
        parametrosDaConsulta.add(pagina * tamanho);
        parametrosDaConsulta.add(tamanho);

        List<Produto> encontrados;
        try (PreparedStatement stm = conexao().prepareStatement(
                "select " + COLUNAS + DE + onde + ordem + " offset ? rows fetch next ? rows only")) {
            vincular(stm, parametrosDaConsulta);
            encontrados = lista(stm);
        }

        return new Pagina<>(encontrados, pagina, tamanho, total, (int) Math.ceil((double) total / tamanho));
    }

    private String condicoes(FiltroDeProdutos filtro, List<Object> parametros) {
        StringBuilder onde = new StringBuilder(" where 1 = 1");

        if (filtro.temTermo()) {
            /*
             * Os dois lados dobrados: o catalogo tem acento e a maioria das pessoas digita sem,
             * ainda mais no celular. Sem isso, "lampada" nao acha "Lâmpada" - o LIKE nao casa e
             * o Jaro-Winkler da 69 contra o corte de 70. Ver D-73.
             */
            onde.append(" and (" + dobrado("p.nome") + " like '%' || " + dobrado("?") + " || '%'"
                    + " or utl_match.jaro_winkler_similarity("
                    + dobrado("p.nome") + ", " + dobrado("?") + ") > 70)");
            parametros.add(filtro.termo());
            parametros.add(filtro.termo());
        }
        if (filtro.secao() != null) {
            // Dobrado tambem: um link digitado a mao com "Decoracao" nao pode devolver vazio so
            // por falta de cedilha.
            onde.append(" and " + dobrado("m.corredor") + " = " + dobrado("?"));
            parametros.add(filtro.secao());
        }
        if (filtro.apenasDisponiveis()) {
            onde.append(" and p.saldo_estoque > 0");
        }

        /*
         * Um EXISTS por caracteristica escolhida. Valores da mesma chave viram um IN, portanto
         * "ou"; chaves diferentes viram EXISTS separados ligados por AND, portanto "e".
         */
        int indice = 0;
        for (Map.Entry<AtributoProduto, List<String>> escolha : filtro.atributos().entrySet()) {
            String marcadores = String.join(", ", escolha.getValue().stream().map(v -> "?").toList());
            onde.append(" " + """
                     and exists (select 1 from tb_produto_atributo a%1$d
                                  where a%1$d.produto_id = p.id
                                    and a%1$d.chave = ?
                                    and a%1$d.valor in (%2$s))""".formatted(indice, marcadores));
            parametros.add(escolha.getKey().name());
            parametros.addAll(escolha.getValue());
            indice++;
        }

        return onde.toString();
    }

    // ---------------------------------------------------------------- navegacao e facetas

    public List<SecaoDoCatalogo> listarSecoes() throws SQLException {
        String sql = """
                select m.corredor, count(*)
                  from tb_produto p
                  join tb_ponto_mapa m on m.id = p.ponto_mapa_id
                 group by m.corredor
                 order by m.corredor
                """;
        List<SecaoDoCatalogo> secoes = new ArrayList<>();
        try (PreparedStatement stm = conexao().prepareStatement(sql);
             ResultSet result = stm.executeQuery()) {
            while (result.next()) {
                secoes.add(new SecaoDoCatalogo(result.getString(1), result.getLong(2)));
            }
        }
        return secoes;
    }

    public List<FacetaDeProdutos> calcularFacetas(FiltroDeProdutos filtro) throws SQLException {
        List<Object> parametros = new ArrayList<>();
        String sql = "select a.chave, a.valor, count(*)" + DE
                + " join tb_produto_atributo a on a.produto_id = p.id"
                + condicoes(filtro, parametros)
                + " group by a.chave, a.valor";

        Map<AtributoProduto, List<FacetaDeProdutos.Valor>> porAtributo = new EnumMap<>(AtributoProduto.class);

        try (PreparedStatement stm = conexao().prepareStatement(sql)) {
            vincular(stm, parametros);
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    porAtributo.computeIfAbsent(
                                    AtributoProduto.valueOf(result.getString(1)), chave -> new ArrayList<>())
                            .add(new FacetaDeProdutos.Valor(result.getString(2), result.getLong(3)));
                }
            }
        }

        /*
         * O EnumMap ja itera na ordem de declaracao do enum, que e a ordem de exibicao: marca
         * primeiro, medidas depois. Dentro de cada filtro, o valor mais comum vem antes.
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

    public List<ValorDeAtributo> buscarAtributosDe(UUID produtoId) throws SQLException {
        List<ValorDeAtributo> atributos = new ArrayList<>();
        try (PreparedStatement stm = conexao().prepareStatement(
                "select chave, valor from tb_produto_atributo where produto_id = ? order by chave")) {
            stm.setString(1, produtoId.toString());
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    atributos.add(new ValorDeAtributo(
                            AtributoProduto.valueOf(result.getString("chave")), result.getString("valor")));
                }
            }
        }
        atributos.sort(Comparator.comparing(ValorDeAtributo::atributo));
        return atributos;
    }

    /**
     * Substitui as caracteristicas do produto pelas informadas.
     * <p>
     * Substitui em vez de acumular: a carga roda a cada inicializacao, e sem apagar antes a
     * unicidade de (produto, chave) recusaria a segunda execucao.
     */
    public void salvarAtributos(UUID produtoId, List<ValorDeAtributo> atributos) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(
                "delete from tb_produto_atributo where produto_id = ?")) {
            stm.setString(1, produtoId.toString());
            stm.executeUpdate();
        }
        try (PreparedStatement stm = conexao().prepareStatement(
                "insert into tb_produto_atributo (id, produto_id, chave, valor) values (?, ?, ?, ?)")) {
            for (ValorDeAtributo atributo : atributos) {
                stm.setString(1, UUID.randomUUID().toString());
                stm.setString(2, produtoId.toString());
                stm.setString(3, atributo.atributo().name());
                stm.setString(4, atributo.valor());
                stm.executeUpdate();
            }
        }
    }

    // ---------------------------------------------------------------- ruptura e gravacao

    /**
     * Pre-filtragem espacial da ruptura de estoque (UC-013): produtos disponiveis dentro do raio,
     * com distancia euclidiana no grid da loja calculada no banco.
     * <p>
     * O produto em falta e excluido: ele esta a distancia zero de si mesmo e, como o saldo do
     * sistema pode nao refletir a prateleira vazia, apareceria como o "melhor" substituto de si
     * proprio.
     * <p>
     * A ordem poe AFINIDADE antes de distancia: mesmo tipo primeiro, depois mesma marca, e so
     * entao proximidade. Sem isso, produtos da mesma secao empatam em distancia e o desempate vira
     * o nome. Ver D-68. Tipo e marca nulos nao quebram nada: a comparacao com nulo nunca e
     * verdadeira, e a ordem cai para distancia e nome.
     */
    public List<Produto> buscarDisponiveisProximosDe(PontoMapa referencia, UUID excluido,
                                                     AfinidadeDeProduto afinidade,
                                                     double raio, int limite) throws SQLException {
        String sql = "select " + COLUNAS + DE + " " + """
                 where p.saldo_estoque > 0
                   and p.id <> ?
                   and sqrt(power(m.coordenada_x - ?, 2) + power(m.coordenada_y - ?, 2)) <= ?
                 order by
                   case when exists (select 1 from tb_produto_atributo a
                                      where a.produto_id = p.id
                                        and a.chave = 'TIPO' and a.valor = ?) then 0 else 1 end,
                   case when exists (select 1 from tb_produto_atributo a
                                      where a.produto_id = p.id
                                        and a.chave = 'MARCA' and a.valor = ?) then 0 else 1 end,
                   sqrt(power(m.coordenada_x - ?, 2) + power(m.coordenada_y - ?, 2)),
                   p.nome
                 fetch first ? rows only
                """;
        try (PreparedStatement stm = conexao().prepareStatement(sql)) {
            vincular(stm, List.<Object>of(
                    excluido.toString(),
                    referencia.getCoordenadaX(), referencia.getCoordenadaY(), raio));
            texto(stm, 5, afinidade.tipo());
            texto(stm, 6, afinidade.marca());
            stm.setInt(7, referencia.getCoordenadaX());
            stm.setInt(8, referencia.getCoordenadaY());
            stm.setInt(9, limite);
            return lista(stm);
        }
    }

    /** Insere o produto, ou atualiza se o id ja existir. Nao grava o ponto do mapa. */
    public Produto salvar(Produto produto) throws SQLException {
        String sql = """
                merge into tb_produto d
                using (select ? id from dual) s
                   on (d.id = s.id)
                 when matched then update set
                      sku = ?, nome = ?, descricao = ?, imagem_url = ?, preco = ?, saldo_estoque = ?,
                      ponto_mapa_id = ?
                 when not matched then insert
                      (id, sku, nome, descricao, imagem_url, preco, saldo_estoque, ponto_mapa_id)
                      values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stm = conexao().prepareStatement(sql)) {
            stm.setString(1, produto.getId().toString());
            preencherDados(stm, 2, produto);
            stm.setString(9, produto.getId().toString());
            preencherDados(stm, 10, produto);
            stm.executeUpdate();
        }
        return produto;
    }

    // ---------------------------------------------------------------- leitura

    /** Le um produto, com o ponto, de uma linha com as colunas de {@link #COLUNAS}. */
    static Produto ler(ResultSet result) throws SQLException {
        return new Produto(
                UUID.fromString(result.getString("p_id")),
                result.getString("p_sku"),
                result.getString("p_nome"),
                result.getString("p_descricao"),
                result.getString("p_imagem_url"),
                result.getBigDecimal("p_preco"),
                result.getInt("p_saldo_estoque"),
                PontoMapaDao.ler(result, "m"));
    }

    private Optional<Produto> buscarUm(String condicao, String valor) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(
                "select " + COLUNAS + DE + " where " + condicao)) {
            stm.setString(1, valor);
            try (ResultSet result = stm.executeQuery()) {
                return result.next() ? Optional.of(ler(result)) : Optional.empty();
            }
        }
    }

    private List<Produto> lista(PreparedStatement stm) throws SQLException {
        stm.setFetchSize(100);
        List<Produto> produtos = new ArrayList<>();
        try (ResultSet result = stm.executeQuery()) {
            while (result.next()) {
                produtos.add(ler(result));
            }
        }
        return produtos;
    }

    private void preencherDados(PreparedStatement stm, int inicio, Produto produto) throws SQLException {
        stm.setString(inicio, produto.getSku());
        stm.setString(inicio + 1, produto.getNome());
        texto(stm, inicio + 2, produto.getDescricao());
        texto(stm, inicio + 3, produto.getImagemUrl());
        stm.setBigDecimal(inicio + 4, produto.getPreco());
        stm.setInt(inicio + 5, produto.getSaldoEstoque());
        stm.setString(inicio + 6, produto.getPontoMapa().getId().toString());
    }

    private static void texto(PreparedStatement stm, int posicao, String valor) throws SQLException {
        if (valor == null) {
            stm.setNull(posicao, Types.VARCHAR);
        } else {
            stm.setString(posicao, valor);
        }
    }

    private static void vincular(PreparedStatement stm, List<Object> parametros) throws SQLException {
        for (int i = 0; i < parametros.size(); i++) {
            Object valor = parametros.get(i);
            if (valor instanceof Integer inteiro) {
                stm.setInt(i + 1, inteiro);
            } else if (valor instanceof Double decimal) {
                stm.setDouble(i + 1, decimal);
            } else {
                stm.setString(i + 1, (String) valor);
            }
        }
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
