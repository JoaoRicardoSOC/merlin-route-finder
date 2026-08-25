package br.com.jence.backend.infrastructure.database.schema;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.TipoPonto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mantem alinhada com o codigo a restricao de verificacao que o Hibernate cria sobre colunas
 * de enum.
 * <p>
 * <b>Por que isso existe.</b> Para uma coluna {@code @Enumerated(EnumType.STRING)}, o Hibernate
 * gera um {@code check (coluna in ('A','B'))} na criacao da tabela. Mas {@code ddl-auto: update}
 * so acrescenta: ele nunca altera nem remove o que ja esta la. Entao, no dia em que um valor
 * entra ou sai do enum, a restricao continua com a lista antiga e o banco passa a recusar
 * gravacoes perfeitamente validas - com um {@code ORA-02290} que nao diz qual coluna nem por que.
 * <p>
 * Foi exatamente o que aconteceu ao trocar {@code TOTEM} por {@code QR_CODE}. Sem Flyway
 * ({@code D-16}) nao ha migracao onde corrigir isso, e cada integrante tem o proprio schema -
 * alem do publicado. Pedir que cada um rode um {@code ALTER} a mao deixaria o ambiente de
 * alguem quebrado sem aviso.
 * <p>
 * <b>Por que reconstruir em vez de so apagar.</b> Apagar resolveria o erro e perderia a
 * garantia. Reconstruir a partir de {@code values()} mantem a protecao no banco e faz a
 * proxima mudanca de enum se resolver sozinha.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RestricaoDeEnumNoBanco {

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

    private final JdbcTemplate jdbc;

    public void sincronizar() {
        COLUNAS.forEach(alvo -> {
            try {
                sincronizar(alvo);
            } catch (RuntimeException e) {
                /*
                 * Um ajuste de esquema que falha nao pode impedir a aplicacao de subir: se a
                 * restricao ja estiver correta - o caso comum - nada disso era necessario, e se
                 * estiver errada o erro aparece na primeira gravacao, com este aviso no log.
                 */
                log.warn("Nao foi possivel sincronizar a restricao de {}: {}", alvo, e.toString());
            }
        });
    }

    private void sincronizar(ColunaDeEnum alvo) {
        String tabela = alvo.tabela();
        String coluna = alvo.coluna();

        Set<String> desejados = Arrays.stream(alvo.enumerado().getEnumConstants())
                .map(Enum::name)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Map<String, Object>> encontradas = jdbc.queryForList("""
                select constraint_name, search_condition_vc
                  from user_constraints
                 where table_name = ?
                   and constraint_type = 'C'
                   and lower(search_condition_vc) like ?
                """, tabela, "%" + coluna.toLowerCase() + " in (%");

        for (Map<String, Object> restricao : encontradas) {
            String nome = (String) restricao.get("CONSTRAINT_NAME");
            Set<String> atuais = literaisDe(String.valueOf(restricao.get("SEARCH_CONDITION_VC")));

            if (atuais.equals(desejados)) {
                return;
            }

            log.info("Restricao {} de {}.{} esta desatualizada: {} no banco, {} no codigo. Refazendo.",
                    nome, tabela, coluna, atuais, desejados);
            jdbc.execute("alter table %s drop constraint %s".formatted(tabela, nome));
        }

        jdbc.execute("alter table %s add check (%s in (%s))".formatted(
                tabela, coluna, desejados.stream()
                        .map("'%s'"::formatted)
                        .collect(Collectors.joining(","))));

        if (encontradas.isEmpty()) {
            log.info("Restricao de {}.{} nao existia e foi criada com {}.", tabela, coluna, desejados);
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
}
