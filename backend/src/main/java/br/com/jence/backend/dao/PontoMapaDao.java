package br.com.jence.backend.dao;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.factory.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Acesso a tabela {@code tb_ponto_mapa}: prateleiras, caixas, banheiros e placas de QR Code.
 * <p>
 * Usa a conexao da transacao em andamento ({@link ConnectionFactory#getConnection()}) e nunca a
 * fecha: quem abre e fecha a conexao e a transacao. Aqui so se fecham os comandos e resultados.
 */
// Provisorio: so enquanto o Spring injeta as dependencias. Sai na fase 10 do plano.
public class PontoMapaDao {

    /** Colunas na ordem que {@link #ler(ResultSet, String)} espera, com o prefixo da tabela. */
    static final String COLUNAS = "%1$s.id %1$s_id, %1$s.tipo %1$s_tipo, %1$s.corredor %1$s_corredor, "
            + "%1$s.coordenada_x %1$s_coordenada_x, %1$s.coordenada_y %1$s_coordenada_y, "
            + "%1$s.codigo_curto %1$s_codigo_curto";

    private static final String SELECT = "select " + COLUNAS.formatted("m") + " from tb_ponto_mapa m";

    public Optional<PontoMapa> buscarPorId(UUID id) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(SELECT + " where m.id = ?")) {
            stm.setString(1, id.toString());
            return primeiro(stm);
        }
    }

    public List<PontoMapa> buscarPorTipo(TipoPonto tipo) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(SELECT + " where m.tipo = ?")) {
            stm.setString(1, tipo.name());
            return lista(stm);
        }
    }

    /**
     * Resolve o codigo digitado pelo cliente, em qualquer grafia.
     *
     * @see PontoMapa#normalizarCodigo(String)
     */
    public Optional<PontoMapa> buscarPorCodigoCurto(String codigoCurto) throws SQLException {
        String canonico = PontoMapa.normalizarCodigo(codigoCurto);
        if (canonico == null) {
            return Optional.empty();
        }
        try (PreparedStatement stm = conexao().prepareStatement(SELECT + " where m.codigo_curto = ?")) {
            stm.setString(1, canonico);
            return primeiro(stm);
        }
    }

    /** Insere o ponto, ou atualiza se o id ja existir. */
    public PontoMapa salvar(PontoMapa ponto) throws SQLException {
        String sql = """
                merge into tb_ponto_mapa d
                using (select ? id from dual) s
                   on (d.id = s.id)
                 when matched then update set
                      tipo = ?, corredor = ?, coordenada_x = ?, coordenada_y = ?, codigo_curto = ?
                 when not matched then insert
                      (id, tipo, corredor, coordenada_x, coordenada_y, codigo_curto)
                      values (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stm = conexao().prepareStatement(sql)) {
            stm.setString(1, ponto.getId().toString());
            preencherDados(stm, 2, ponto);
            stm.setString(7, ponto.getId().toString());
            preencherDados(stm, 8, ponto);
            stm.executeUpdate();
        }
        return ponto;
    }

    /**
     * Apaga pontos gravados com um tipo que {@link TipoPonto} nao tem mais.
     * <p>
     * Pelo texto da coluna, sem converter para o enum: a conversao falharia justamente nas linhas
     * que precisam sair.
     */
    public int apagarPorTipoBruto(String tipo) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement("delete from tb_ponto_mapa where tipo = ?")) {
            stm.setString(1, tipo);
            return stm.executeUpdate();
        }
    }

    /**
     * Renomeia um corredor no lugar, sem criar ponto novo.
     * <p>
     * A carga casa secao existente pelo nome do corredor, entao mudar o nome na planta faria ela
     * criar um ponto novo e vazio - e os produtos, presos ao ponto antigo pela chave estrangeira,
     * ficariam na secao de nome velho. Atualizar a linha preserva o id. Ver D-70.
     */
    public int renomearCorredor(String antigo, String novo) throws SQLException {
        try (PreparedStatement stm = conexao().prepareStatement(
                "update tb_ponto_mapa set corredor = ? where corredor = ?")) {
            stm.setString(1, novo);
            stm.setString(2, antigo);
            return stm.executeUpdate();
        }
    }

    /**
     * Apaga uma prateleira que ficou com o nome de destino e nenhum produto dentro.
     * <p>
     * Se a versao com o nome novo na planta rodar antes da que renomeia, a carga cria a secao nova
     * vazia e o corredor passa a existir duas vezes. Renomear por cima produziria duas linhas com
     * o mesmo nome, uma com todos os produtos e outra com nenhum. Apagar a vazia e seguro:
     * prateleira sem produto nao e referenciada por ninguem.
     */
    public int apagarPrateleiraVaziaChamada(String corredor) throws SQLException {
        String sql = """
                delete from tb_ponto_mapa
                 where corredor = ?
                   and tipo = 'PRATELEIRA'
                   and id not in (select ponto_mapa_id from tb_produto where ponto_mapa_id is not null)
                """;
        try (PreparedStatement stm = conexao().prepareStatement(sql)) {
            stm.setString(1, corredor);
            return stm.executeUpdate();
        }
    }

    /**
     * Le um ponto de uma linha cujas colunas vieram de {@link #COLUNAS} com o prefixo informado.
     * Usado tambem pelos DAOs que trazem o ponto junto, num {@code join}.
     */
    static PontoMapa ler(ResultSet result, String prefixo) throws SQLException {
        String id = result.getString(prefixo + "_id");
        if (id == null) {
            return null;
        }
        return new PontoMapa(
                UUID.fromString(id),
                TipoPonto.valueOf(result.getString(prefixo + "_tipo")),
                result.getString(prefixo + "_corredor"),
                result.getInt(prefixo + "_coordenada_x"),
                result.getInt(prefixo + "_coordenada_y"),
                result.getString(prefixo + "_codigo_curto"));
    }

    private void preencherDados(PreparedStatement stm, int inicio, PontoMapa ponto) throws SQLException {
        stm.setString(inicio, ponto.getTipo().name());
        stm.setString(inicio + 1, ponto.getCorredor());
        stm.setInt(inicio + 2, ponto.getCoordenadaX());
        stm.setInt(inicio + 3, ponto.getCoordenadaY());
        if (ponto.getCodigoCurto() == null) {
            stm.setNull(inicio + 4, Types.VARCHAR);
        } else {
            stm.setString(inicio + 4, ponto.getCodigoCurto());
        }
    }

    private Optional<PontoMapa> primeiro(PreparedStatement stm) throws SQLException {
        try (ResultSet result = stm.executeQuery()) {
            return result.next() ? Optional.of(ler(result, "m")) : Optional.empty();
        }
    }

    private List<PontoMapa> lista(PreparedStatement stm) throws SQLException {
        stm.setFetchSize(100);
        List<PontoMapa> pontos = new ArrayList<>();
        try (ResultSet result = stm.executeQuery()) {
            while (result.next()) {
                pontos.add(ler(result, "m"));
            }
        }
        return pontos;
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
