package br.com.jence.backend.application.usecase;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.model.*;
import br.com.jence.backend.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.model.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.ExecutorDeFerramenta;
import br.com.jence.backend.model.MensagemIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversarComAssistenteUseCaseTest {

    @Mock SessaoRepository sessaoRepository;
    @Mock ChatMensagemRepository chatMensagemRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock AssistenteIA assistenteIA;
    @InjectMocks ConversarComAssistenteUseCase useCase;

    private UUID sessaoId;

    @BeforeEach
    void preparar() {
        sessaoId = UUID.randomUUID();
    }

    private void comSessaoAtiva() {
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(Sessao.iniciar(sessaoId)));
    }

    private void aoSalvarDevolverAMensagem() {
        when(chatMensagemRepository.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    @DisplayName("persiste a pergunta e a resposta, e devolve a resposta")
    void conversaCompleta() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());
        when(assistenteIA.conversar(any(), any(), any(), any()))
                .thenReturn("Voce vai precisar de tinta, rolo e lixa.");

        ChatMensagemResponse resposta = useCase.executar(sessaoId, "o que preciso para pintar?");

        assertEquals("Voce vai precisar de tinta, rolo e lixa.", resposta.conteudo());
        assertEquals(Remetente.ASSISTANT, resposta.remetente());

        ArgumentCaptor<ChatMensagem> salvas = ArgumentCaptor.forClass(ChatMensagem.class);
        verify(chatMensagemRepository, times(2)).salvar(salvas.capture());

        assertTrue(salvas.getAllValues().get(0).isDoCliente());
        assertEquals("o que preciso para pintar?", salvas.getAllValues().get(0).getConteudo());
        assertEquals(Remetente.ASSISTANT, salvas.getAllValues().get(1).getRemetente());
    }

    @Test
    @DisplayName("repassa o historico anterior ao assistente, na ordem, com a nova pergunta ao fim")
    void repassaHistorico() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of(
                ChatMensagem.doCliente(UUID.randomUUID(), sessaoId, "quero pintar o quarto"),
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "de que cor?")));
        when(assistenteIA.conversar(any(), any(), any(), any())).thenReturn("Entendi.");

        useCase.executar(sessaoId, "branco");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<MensagemIA>> historico = ArgumentCaptor.forClass(List.class);
        verify(assistenteIA).conversar(any(), historico.capture(), any(), any());

        assertEquals(List.of("quero pintar o quarto", "de que cor?", "branco"),
                historico.getValue().stream().map(MensagemIA::conteudo).toList());
    }

    /** O grounding: a ferramenta so devolve o que a nossa busca encontrou. */
    @Test
    @DisplayName("a ferramenta de busca devolve produtos reais do catalogo")
    void ferramentaConsultaOCatalogo() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());

        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto tinta = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica 18L",
                new BigDecimal("289.90"), 12, tintas);
        when(produtoRepository.buscarPorTermo(eq("tinta"), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(tinta), 0, 8, 1L, 1));

        // simula o assistente decidindo usar a ferramenta
        when(assistenteIA.conversar(any(), any(), any(), any())).thenAnswer(invocacao -> {
            ExecutorDeFerramenta executor = invocacao.getArgument(3);
            Map<String, Object> resultado = executor.executar("buscar_produtos", Map.of("termos", "tinta"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> produtos = (List<Map<String, Object>>) resultado.get("produtos");
            assertEquals(1, produtos.size());
            assertEquals("Tinta Acrilica 18L", produtos.get(0).get("nome"));
            assertEquals("Tintas", produtos.get(0).get("corredor"));
            assertEquals(true, produtos.get(0).get("disponivel"));

            return "A Tinta Acrilica 18L esta no corredor Tintas.";
        });

        assertTrue(useCase.executar(sessaoId, "quero tinta").conteudo().contains("Tinta Acrilica 18L"));
    }

    /** Uma chamada da ferramenta cobre varios termos: o tier gratuito so permite 5 por minuto. */
    @Test
    @DisplayName("varios termos numa chamada so, sem repetir produto")
    void ferramentaAceitaVariosTermos() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());

        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto tinta = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica 18L",
                new BigDecimal("289.90"), 12, tintas);
        Produto rolo = new Produto(UUID.randomUUID(), "SKU-TIN-002", "Rolo de La 23cm",
                new BigDecimal("34.90"), 25, tintas);

        when(produtoRepository.buscarPorTermo(eq("tinta"), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(tinta), 0, 8, 1L, 1));
        // "pintura" devolve os dois: a tinta ja veio antes e nao pode repetir
        when(produtoRepository.buscarPorTermo(eq("pintura"), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(tinta, rolo), 0, 8, 2L, 1));

        when(assistenteIA.conversar(any(), any(), any(), any())).thenAnswer(invocacao -> {
            ExecutorDeFerramenta executor = invocacao.getArgument(3);
            Map<String, Object> resultado = executor.executar("buscar_produtos",
                    Map.of("termos", "tinta, pintura"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> produtos = (List<Map<String, Object>>) resultado.get("produtos");
            assertEquals(2, produtos.size());
            assertEquals(List.of("Tinta Acrilica 18L", "Rolo de La 23cm"),
                    produtos.stream().map(p -> p.get("nome")).toList());
            return "ok";
        });

        useCase.executar(sessaoId, "quero pintar");

        // uma unica ida ao assistente, apesar dos dois termos
        verify(assistenteIA, times(1)).conversar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("termo vazio na ferramenta nao consulta o banco")
    void ferramentaComTermoVazio() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());
        when(assistenteIA.conversar(any(), any(), any(), any())).thenAnswer(invocacao -> {
            ExecutorDeFerramenta executor = invocacao.getArgument(3);
            assertEquals(List.of(), executor.executar("buscar_produtos", Map.of()).get("produtos"));
            return "ok";
        });

        useCase.executar(sessaoId, "oi");

        verify(produtoRepository, never()).buscarPorTermo(any(), anyInt(), anyInt());
    }

    /** Aplicacao da D-35: o fallback e daqui, e a falha nao vira registro. */
    @Test
    @DisplayName("IA indisponivel devolve mensagem amigavel e nao persiste a falha")
    void iaIndisponivel() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());
        when(assistenteIA.conversar(any(), any(), any(), any()))
                .thenThrow(new AssistenteIAIndisponivelException("cota esgotada"));

        ChatMensagemResponse resposta = useCase.executar(sessaoId, "o que preciso para pintar?");

        assertTrue(resposta.conteudo().contains("Não consegui consultar o assistente"));

        // so a pergunta do cliente foi salva: a falha nao entra no historico
        verify(chatMensagemRepository, times(1)).salvar(any());
    }

    @Test
    @DisplayName("sessao inativa e recusada")
    void sessaoInativa() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(encerrada));

        assertThrows(OperacaoNaoPermitidaException.class, () -> useCase.executar(sessaoId, "oi"));

        verifyNoInteractions(assistenteIA);
    }

    @Test
    @DisplayName("sessao inexistente devolve nao encontrado")
    void sessaoInexistente() {
        when(sessaoRepository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.executar(sessaoId, "oi"));
    }
}
