package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.RegistroRuptura;
import br.com.jence.backend.model.Sessao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * {@link RegistroRupturaDao} contra o Oracle.
 * <p>
 * O relato e gravado numa transacao propria, entao ele sobrevive ao rollback de quem chamou - e
 * e isso que o teste verifica. Como ele fica no banco de verdade, o teste cria a propria sessao,
 * confirmada, e apaga tudo no fim.
 */
@Tag("integracao")
class RegistroRupturaDaoIntegracaoTest {

    private final RegistroRupturaDao dao = new RegistroRupturaDao();
    private UUID sessaoId;
    private Produto emFalta;

    @BeforeEach
    void montar() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            emFalta = new ProdutoDao().buscarPorSku("SKU-FER-002").orElseThrow();
            sessaoId = new SessaoDao().salvar(Sessao.iniciar(UUID.randomUUID())).getId();
            transacao.confirmar();
        }
    }

    @AfterEach
    void limpar() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            for (String sql : new String[]{
                    "delete from tb_ch26_registro_ruptura where sessao_id = ?",
                    "delete from tb_ch26_sessao where id = ?"}) {
                try (PreparedStatement stm = ConnectionFactory.getConnection().prepareStatement(sql)) {
                    stm.setString(1, sessaoId.toString());
                    stm.executeUpdate();
                }
            }
            transacao.confirmar();
        }
    }

    @Test
    @DisplayName("o relato sobrevive ao rollback da operacao que o registrou (D-39)")
    void sobreviveAoRollback() throws Exception {
        RegistroRuptura registro = RegistroRuptura.semSugestao(
                sessaoId, UUID.randomUUID(), emFalta.getId(), "Nenhum vizinho disponivel no raio");

        try (Transacao operacao = Transacao.iniciar()) {
            dao.salvar(registro);
            // sai sem confirmar: a operacao inteira e desfeita
        }

        List<RegistroRuptura> registros;
        try (Transacao leitura = Transacao.iniciarSomenteLeitura()) {
            registros = dao.buscarPorSessao(sessaoId);
        }

        assertEquals(1, registros.size());
        assertEquals(registro.getId(), registros.getFirst().getId());
        assertEquals(OrigemSugestao.NENHUMA, registros.getFirst().getOrigem());
        assertNull(registros.getFirst().getProdutoSugeridoId());
        assertEquals("Nenhum vizinho disponivel no raio", registros.getFirst().getJustificativa());
        assertEquals(registro.getRegistradoEm(), registros.getFirst().getRegistradoEm());
    }
}
