package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.TipoPonto;

import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mantem alinhada com o codigo a restricao {@code CHECK} das colunas de enum.
 * <p>
 * <b>Por que isso existe.</b> Cada coluna de enum tem um {@code check (coluna in ('A','B'))}.
 * Quando um valor entra ou sai do enum, a restricao continua com a lista antiga e o banco passa
 * a recusar gravacoes validas - com um {@code ORA-02290} que nao diz qual coluna nem por que.
 * Foi o que aconteceu ao trocar {@code TOTEM} por {@code QR_CODE}. Cada integrante tem o proprio
 * schema, alem do publicado, e pedir que cada um rode um {@code ALTER} a mao deixaria o ambiente
 * de alguem quebrado sem aviso.
 * <p>
 * <b>Por que reconstruir em vez de so apagar.</b> Apagar resolveria o erro e perderia a garantia.
 * Reconstruir a partir de {@code values()} mantem a protecao e faz a proxima mudanca de enum se
 * resolver sozinha.
 * <p>
 * Atencao: no Oracle, {@code ALTER TABLE} confirma a transacao em andamento. Chame este DAO no
 * inicio de uma transacao, antes de qualquer gravacao.
 */
// Provisorio: so enquanto o Spring injeta as dependencias. Sai na fase 10 do plano.
@Component
public class RestricaoDeEnumDao {

    /** Uma coluna de enum sob cuidado deste mecanismo. */
    private record ColunaDeEnum(String tabela, String coluna, Class<? extends Enum<?>> enumerado) {
        @Override
        public String toString() {
            return tabela + "." + coluna;
        }
    }

    private static final List<ColunaDeEnum> COLUNAS = List.of(
            new ColunaDeEnum("TB_PONTO_MAPA", "TIPO", TipoPonto.class),
            new ColunaDeEnum("TB_PRODUTO_ATRIBUTO", "CHAVE", AtributoProduto.class));

    /** Captura cada literal entre aspas simples da condicao da restricao. */
    private static final Pattern LITERAL = Pattern.compile("'([^']*)'");

    public void sincronizar() {
        for (ColunaDeEnum alvo : COLUNAS) {
            try {
                sincronizar(alvo);
            } catch (SQLException | RuntimeException e) {
                /*
                 * Um ajuste de esquema que falha nao pode impedir a aplicacao de subir: se a
                 * restricao ja estiver correta - o caso comum - nada disso era necessario, e se
                 * estiver errada o erro aparece na primeira gravacao, com este aviso no log.
                 */
                System.out.println(String.format(
                        "AVISO: Nao foi possivel sincronizar a restricao de %s: %s", alvo, e));
            }
        }
    }

    private void sincronizar(ColunaDeEnum alvo) throws SQLException {
        String tabela = alvo.tabela();
        String coluna = alvo.coluna();

        Set<String> desejados = Arrays.stream(alvo.enumerado().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, String> encontradas = new LinkedHashMap<>();
        try (PreparedStatement stm = conexao().prepareStatement("""
                select constraint_name, search_condition_vc
                  from user_constraints
                 where table_name = ?
                   and constraint_type = 'C'
                   and lower(search_condition_vc) like ?
                """)) {
            stm.setString(1, tabela);
            stm.setString(2, "%" + coluna.toLowerCase() + " in (%");
            try (ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    encontradas.put(result.getString("constraint_name"), result.getString("search_condition_vc"));
                }
            }
        }

        try (Statement ddl = conexao().createStatement()) {
            for (Map.Entry<String, String> restricao : encontradas.entrySet()) {
                Set<String> atuais = literaisDe(String.valueOf(restricao.getValue()));

                if (atuais.equals(desejados)) {
                    return;
                }

                System.out.println(String.format(
                        "Restricao %s de %s.%s esta desatualizada: %s no banco, %s no codigo. Refazendo.",
                        restricao.getKey(), tabela, coluna, atuais, desejados));
                ddl.execute("alter table %s drop constraint %s".formatted(tabela, restricao.getKey()));
            }

            ddl.execute("alter table %s add check (%s in (%s))".formatted(
                    tabela, coluna, desejados.stream()
                            .map("'%s'"::formatted)
                            .collect(Collectors.joining(","))));
        }

        if (encontradas.isEmpty()) {
            System.out.println(String.format(
                    "Restricao de %s.%s nao existia e foi criada com %s.", tabela, coluna, desejados));
        }
    }

    private Set<String> literaisDe(String condicao) {
        Set<String> literais = new LinkedHashSet<>();
        Matcher m = LITERAL.matcher(condicao);
        while (m.find()) {
            literais.add(m.group(1));
        }
        return literais;
    }

    private Connection conexao() {
        return ConnectionFactory.getConnection();
    }
}
