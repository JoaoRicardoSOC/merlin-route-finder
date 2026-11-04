package br.com.jence.backend.application.usecase;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarHistoricoChatUseCaseTest {

    @Mock SessaoRepository sessaoRepository;
    @Mock ChatMensagemRepository chatMensagemRepository;
    @InjectMocks ConsultarHistoricoChatUseCase useCase;

    private UUID sessaoId;

    @BeforeEach
    void preparar() {
        sessaoId = UUID.randomUUID();
    }

    @Test
    @DisplayName("devolve a conversa preservando a ordem do repositorio")
    void devolveHistorico() {
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(Sessao.iniciar(sessaoId)));
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of(
                ChatMensagem.doCliente(UUID.randomUUID(), sessaoId, "o que preciso para pintar?"),
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "tinta, rolo e lixa")));

        List<ChatMensagemResponse> historico = useCase.executar(sessaoId);

        assertEquals(2, historico.size());
        assertEquals("o que preciso para pintar?", historico.getFirst().conteudo());
        assertEquals("tinta, rolo e lixa", historico.getLast().conteudo());
    }

    @Test
    @DisplayName("sessao encerrada ainda devolve o historico: e registro do que aconteceu")
    void sessaoEncerradaAindaLe() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(encerrada));
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of(
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "leve a lixa d'agua")));

        assertEquals(1, useCase.executar(sessaoId).size());
    }

    @Test
    @DisplayName("consultar nao renova o TTL: leitura nao deveria gravar nada")
    void naoRenovaSessao() {
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(Sessao.iniciar(sessaoId)));
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());

        useCase.executar(sessaoId);

        verify(sessaoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("sessao inexistente devolve 404 em vez de lista vazia")
    void sessaoInexistente() {
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.executar(sessaoId),
                "lista vazia seria indistinguivel de uma conversa que ainda nao comecou");

        verify(chatMensagemRepository, never()).buscarHistorico(any());
    }
}
