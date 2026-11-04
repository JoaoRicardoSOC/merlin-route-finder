package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RestricaoDeEnumDao} contra o Oracle.
 * <p>
 * Com o schema ja alinhado - o caso de todo dia -, sincronizar nao executa DDL nenhum. O teste
 * confere que, depois de sincronizar, a restricao do tipo de ponto aceita cada valor do enum.
 */
@Tag("integracao")
class RestricaoDeEnumDaoIntegracaoTest {

    @Test
    @DisplayName("depois de sincronizar, a restricao do tipo de ponto lista todos os valores do enum")
    void sincroniza() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            new RestricaoDeEnumDao().sincronizar();

            String condicao = null;
            try (PreparedStatement stm = ConnectionFactory.getConnection().prepareStatement("""
                    select search_condition_vc from user_constraints
                     where table_name = 'TB_PONTO_MAPA' and constraint_type = 'C'
                       and lower(search_condition_vc) like 'tipo in (%'
                    """);
                 ResultSet result = stm.executeQuery()) {
                while (result.next()) {
                    condicao = result.getString(1);
                }
            }

            for (TipoPonto tipo : TipoPonto.values()) {
                assertTrue(condicao != null && condicao.contains("'" + tipo.name() + "'"),
                        "a restricao precisa aceitar " + tipo + ": " + condicao);
            }
            transacao.confirmar();
        }
    }
}
