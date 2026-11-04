package br.com.jence.backend.dao;

import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** {@link SessaoDao} contra o Oracle, numa transacao que nunca e confirmada. */
@Tag("integracao")
class SessaoDaoIntegracaoTest {

    private final SessaoDao dao = new SessaoDao();
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
    @DisplayName("sessao com placa: grava e le de volta, com o ponto e as datas exatas")
    void comPlaca() throws Exception {
        PontoMapa placa = new PontoMapaDao().buscarPorCodigoCurto("ENT-01").orElseThrow();
        Sessao sessao = Sessao.iniciar(UUID.randomUUID(), placa);

        dao.salvar(sessao);
        Sessao lida = dao.buscarPorId(sessao.getId()).orElseThrow();

        assertEquals(StatusSessao.ACTIVE, lida.getStatus());
        assertEquals(sessao.getCriadoEm(), lida.getCriadoEm());
        assertEquals(sessao.getExpiracaoTtl(), lida.getExpiracaoTtl());
        assertEquals(sessao.getEscaneadoEm(), lida.getEscaneadoEm());
        assertEquals(placa.getId(), lida.getPontoEscaneado().getId());
    }

    @Test
    @DisplayName("sessao sem placa le o ponto como nulo")
    void semPlaca() throws Exception {
        Sessao sessao = Sessao.iniciar(UUID.randomUUID());

        dao.salvar(sessao);
        Sessao lida = dao.buscarPorId(sessao.getId()).orElseThrow();

        assertNull(lida.getPontoEscaneado());
        assertNull(lida.getEscaneadoEm());
        assertTrue(dao.buscarPorId(UUID.randomUUID()).isEmpty());
    }

    @Test
    @DisplayName("salvar de novo atualiza, e as vencidas incluem so as ativas com TTL passado")
    void atualizaEBuscaVencidas() throws Exception {
        LocalDateTime longeNoPassado = LocalDateTime.of(2000, 1, 1, 0, 0);
        Sessao vencida = Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                longeNoPassado, longeNoPassado.plusHours(4), null, null);
        Sessao encerrada = Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                longeNoPassado, longeNoPassado.plusHours(4), null, null);
        dao.salvar(vencida);
        dao.salvar(encerrada);

        encerrada.encerrar();
        dao.salvar(encerrada);

        assertEquals(StatusSessao.COMPLETED, dao.buscarPorId(encerrada.getId()).orElseThrow().getStatus());

        var vencidas = dao.buscarExpiradas(longeNoPassado.plusHours(5)).stream().map(Sessao::getId).toList();
        assertTrue(vencidas.contains(vencida.getId()));
        assertFalse(vencidas.contains(encerrada.getId()));
    }
}
