package br.com.jence.backend.service;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

/**
 * Historico da conversa com o assistente, em ordem cronologica.
 * <p>
 * Segue a mesma regra do {@link ConsultarListaRoteiroUseCase}: consultar e uma leitura e nao
 * renova o TTL da sessao. A renovacao acontece ao enviar mensagem, que e a acao que de fato
 * indica um cliente ativo.
 * <p>
 * A sessao encerrada <b>nao</b> impede a leitura. O historico e registro do que ja aconteceu:
 * negar acesso a ele depois que a jornada termina nao protege nada e quebraria o celular que
 * reabre a conversa para reler uma recomendacao.
 */
public class ConsultarHistoricoChatUseCase {

    private final SessaoDao sessaoDao;
    private final ChatMensagemDao chatMensagemDao;

    public ConsultarHistoricoChatUseCase(SessaoDao sessaoDao, ChatMensagemDao chatMensagemDao) {
        this.sessaoDao = sessaoDao;
        this.chatMensagemDao = chatMensagemDao;
    }

    public List<ChatMensagemResponse> executar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            List<ChatMensagemResponse> historico = consultar(sessaoId);
            transacao.confirmar();
            return historico;
        }
    }

    private List<ChatMensagemResponse> consultar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        /*
         * A sessao e verificada mesmo sem ser usada depois. Sem isso, um id inexistente
         * devolveria lista vazia - indistinguivel de uma conversa que ainda nao comecou, e o
         * frontend nao teria como saber que estava perguntando pelo lugar errado.
         */
        if (sessaoDao.buscarPorId(sessaoId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Sessao", sessaoId);
        }

        return chatMensagemDao.buscarHistorico(sessaoId).stream()
                .map(ChatMensagemResponse::de)
                .toList();
    }
}
