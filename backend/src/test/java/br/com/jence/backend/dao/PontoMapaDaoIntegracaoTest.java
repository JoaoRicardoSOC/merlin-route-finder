package br.com.jence.backend.dao;

import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PontoMapaDao} contra o Oracle.
 * <p>
 * Cada teste roda numa transacao que nunca e confirmada: o que ele grava e desfeito no fim, e o
 * schema - que e o mesmo da instancia publicada - fica como estava.
 */
@Tag("integracao")
class PontoMapaDaoIntegracaoTest {

    private final PontoMapaDao dao = new PontoMapaDao();
    private Transacao transacao;

    @BeforeEach
    void abrir() throws Exception {
        transacao = Transacao.iniciar();
    }

    @AfterEach
    void desfazer() throws Exception {
        transacao.close();
    }

    @Test
    @DisplayName("acha a placa pelo codigo em qualquer grafia")
    void buscaPorCodigoCurto() throws Exception {
        Optional<PontoMapa> placa = dao.buscarPorCodigoCurto("ent-01");

        assertTrue(placa.isPresent());
        assertEquals(TipoPonto.QR_CODE, placa.get().getTipo());
        assertEquals("ENT01", placa.get().getCodigoCurto());
        PontoMapa pelaChave = dao.buscarPorId(placa.get().getId()).orElseThrow();
        assertEquals(placa.get().getCorredor(), pelaChave.getCorredor());
        assertEquals(placa.get().getCoordenadaX(), pelaChave.getCoordenadaX());
        assertEquals(placa.get().getCoordenadaY(), pelaChave.getCoordenadaY());
    }

    @Test
    @DisplayName("codigo vazio ou desconhecido nao acha nada")
    void codigoInexistente() throws Exception {
        assertTrue(dao.buscarPorCodigoCurto("--").isEmpty());
        assertTrue(dao.buscarPorCodigoCurto("XXX-99").isEmpty());
        assertTrue(dao.buscarPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("lista por tipo traz so pontos daquele tipo")
    void buscaPorTipo() throws Exception {
        List<PontoMapa> prateleiras = dao.buscarPorTipo(TipoPonto.PRATELEIRA);

        assertFalse(prateleiras.isEmpty());
        for (PontoMapa ponto : prateleiras) {
            assertEquals(TipoPonto.PRATELEIRA, ponto.getTipo());
        }
    }

    @Test
    @DisplayName("salvar insere ponto novo e, com o mesmo id, atualiza")
    void salvarInsereEAtualiza() throws Exception {
        UUID id = UUID.randomUUID();
        dao.salvar(new PontoMapa(id, TipoPonto.PRATELEIRA, "Corredor de teste", 1, 2, "tst-99"));

        PontoMapa inserido = dao.buscarPorId(id).orElseThrow();
        assertEquals("Corredor de teste", inserido.getCorredor());
        assertEquals("TST99", inserido.getCodigoCurto());

        dao.salvar(new PontoMapa(id, TipoPonto.PRATELEIRA, "Corredor renomeado", 3, 4));

        PontoMapa atualizado = dao.buscarPorId(id).orElseThrow();
        assertEquals("Corredor renomeado", atualizado.getCorredor());
        assertEquals(3, atualizado.getCoordenadaX());
        assertEquals(4, atualizado.getCoordenadaY());
        assertEquals(null, atualizado.getCodigoCurto());
    }

    @Test
    @DisplayName("renomear corredor e apagar prateleira vazia mexem so nas linhas certas")
    void renomearEApagarVazia() throws Exception {
        UUID id = UUID.randomUUID();
        dao.salvar(new PontoMapa(id, TipoPonto.PRATELEIRA, "Corredor provisorio", 1, 1));

        assertEquals(1, dao.renomearCorredor("Corredor provisorio", "Corredor definitivo"));
        assertEquals("Corredor definitivo", dao.buscarPorId(id).orElseThrow().getCorredor());

        assertEquals(1, dao.apagarPrateleiraVaziaChamada("Corredor definitivo"));
        assertTrue(dao.buscarPorId(id).isEmpty());
    }
}
