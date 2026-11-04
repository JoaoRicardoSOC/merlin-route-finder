package br.com.jence.backend.service;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.ConexaoFalsa;
import br.com.jence.backend.factory.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ConsultarHistoricoChatUseCaseTest {

    /** Sessoes em memoria, contando as gravacoes. */
    static class SessaoDaoFalso extends SessaoDao {
        final Map<UUID, Sessao> sessoes = new HashMap<>();
        int salvamentos;

        @Override
        public Optional<Sessao> buscarPorId(UUID id) {
            return Optional.ofNullable(sessoes.get(id));
        }

        @Override
        public Sessao salvar(Sessao sessao) {
            salvamentos++;
            sessoes.put(sessao.getId(), sessao);
            return sessao;
        }
    }

    /** Historico fixo por sessao, contando as consultas. */
    static class ChatMensagemDaoFalso extends ChatMensagemDao {
        final Map<UUID, List<ChatMensagem>> historicos = new HashMap<>();
        int consultas;

        @Override
        public List<ChatMensagem> buscarHistorico(UUID sessaoId) {
            consultas++;
            return historicos.getOrDefault(sessaoId, List.of());
        }
    }

    private SessaoDaoFalso sessaoDao;
    private ChatMensagemDaoFalso chatMensagemDao;
    private ConsultarHistoricoChatUseCase useCase;

    private UUID sessaoId;

    @BeforeEach
    void preparar() {
        ConexaoFalsa.usarNaFabrica();
        sessaoDao = new SessaoDaoFalso();
        chatMensagemDao = new ChatMensagemDaoFalso();
        useCase = new ConsultarHistoricoChatUseCase(sessaoDao, chatMensagemDao);
        sessaoId = UUID.randomUUID();
    }

    @AfterEach
    void restaurar() {
        ConnectionFactory.usarBanco();
    }

    @Test
    @DisplayName("devolve a conversa preservando a ordem do repositorio")
    void devolveHistorico() throws Exception {
        sessaoDao.sessoes.put(sessaoId, Sessao.iniciar(sessaoId));
        chatMensagemDao.historicos.put(sessaoId, List.of(
                ChatMensagem.doCliente(UUID.randomUUID(), sessaoId, "o que preciso para pintar?"),
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "tinta, rolo e lixa")));

        List<ChatMensagemResponse> historico = useCase.executar(sessaoId);

        assertEquals(2, historico.size());
        assertEquals("o que preciso para pintar?", historico.getFirst().conteudo());
        assertEquals("tinta, rolo e lixa", historico.getLast().conteudo());
    }

    @Test
    @DisplayName("sessao encerrada ainda devolve o historico: e registro do que aconteceu")
    void sessaoEncerradaAindaLe() throws Exception {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        sessaoDao.sessoes.put(sessaoId, encerrada);
        chatMensagemDao.historicos.put(sessaoId, List.of(
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "leve a lixa d'agua")));

        assertEquals(1, useCase.executar(sessaoId).size());
    }

    @Test
    @DisplayName("consultar nao renova o TTL: leitura nao deveria gravar nada")
    void naoRenovaSessao() throws Exception {
        sessaoDao.sessoes.put(sessaoId, Sessao.iniciar(sessaoId));

        useCase.executar(sessaoId);

        assertEquals(0, sessaoDao.salvamentos);
    }

    @Test
    @DisplayName("sessao inexistente devolve 404 em vez de lista vazia")
    void sessaoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.executar(sessaoId),
                "lista vazia seria indistinguivel de uma conversa que ainda nao comecou");

        assertEquals(0, chatMensagemDao.consultas);
    }
}
