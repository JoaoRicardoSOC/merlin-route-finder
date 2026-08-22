package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThat(historico).hasSize(2);
        assertThat(historico.getFirst().conteudo()).isEqualTo("o que preciso para pintar?");
        assertThat(historico.getLast().conteudo()).isEqualTo("tinta, rolo e lixa");
    }

    @Test
    @DisplayName("sessao encerrada ainda devolve o historico: e registro do que aconteceu")
    void sessaoEncerradaAindaLe() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(encerrada));
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of(
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "leve a lixa d'agua")));

        assertThat(useCase.executar(sessaoId)).hasSize(1);
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

        assertThatThrownBy(() -> useCase.executar(sessaoId))
                .as("lista vazia seria indistinguivel de uma conversa que ainda nao comecou")
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(chatMensagemRepository, never()).buscarHistorico(any());
    }
}
