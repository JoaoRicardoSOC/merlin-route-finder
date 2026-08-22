package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
@Service
@RequiredArgsConstructor
public class ConsultarHistoricoChatUseCase {

    private final SessaoRepository sessaoRepository;
    private final ChatMensagemRepository chatMensagemRepository;

    public List<ChatMensagemResponse> executar(UUID sessaoId) {
        /*
         * A sessao e verificada mesmo sem ser usada depois. Sem isso, um id inexistente
         * devolveria lista vazia - indistinguivel de uma conversa que ainda nao comecou, e o
         * frontend nao teria como saber que estava perguntando pelo lugar errado.
         */
        if (sessaoRepository.buscarPorId(sessaoId).isEmpty()) {
            throw new RecursoNaoEncontradoException("Sessao", sessaoId);
        }

        return chatMensagemRepository.buscarHistorico(sessaoId).stream()
                .map(ChatMensagemResponse::de)
                .toList();
    }
}
