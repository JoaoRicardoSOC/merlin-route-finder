package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.domain.entity.*;
import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.ExecutorDeFerramenta;
import br.com.jence.backend.domain.service.MensagemIA;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

        assertThat(resposta.conteudo()).isEqualTo("Voce vai precisar de tinta, rolo e lixa.");
        assertThat(resposta.remetente()).isEqualTo(Remetente.ASSISTANT);

        ArgumentCaptor<ChatMensagem> salvas = ArgumentCaptor.forClass(ChatMensagem.class);
        verify(chatMensagemRepository, times(2)).salvar(salvas.capture());

        assertThat(salvas.getAllValues().get(0).isDoCliente()).isTrue();
        assertThat(salvas.getAllValues().get(0).getConteudo()).isEqualTo("o que preciso para pintar?");
        assertThat(salvas.getAllValues().get(1).getRemetente()).isEqualTo(Remetente.ASSISTANT);
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

        assertThat(historico.getValue()).extracting(MensagemIA::conteudo)
                .containsExactly("quero pintar o quarto", "de que cor?", "branco");
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
            assertThat(produtos).hasSize(1);
            assertThat(produtos.get(0))
                    .containsEntry("nome", "Tinta Acrilica 18L")
                    .containsEntry("corredor", "Tintas")
                    .containsEntry("disponivel", true);

            return "A Tinta Acrilica 18L esta no corredor Tintas.";
        });

        assertThat(useCase.executar(sessaoId, "quero tinta").conteudo()).contains("Tinta Acrilica 18L");
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
            assertThat(produtos).hasSize(2);
            assertThat(produtos).extracting(p -> p.get("nome"))
                    .containsExactly("Tinta Acrilica 18L", "Rolo de La 23cm");
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
            assertThat(executor.executar("buscar_produtos", Map.of())).containsEntry("produtos", List.of());
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

        assertThat(resposta.conteudo()).contains("Não consegui consultar o assistente");

        // so a pergunta do cliente foi salva: a falha nao entra no historico
        verify(chatMensagemRepository, times(1)).salvar(any());
    }

    @Test
    @DisplayName("sessao inativa e recusada")
    void sessaoInativa() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(encerrada));

        assertThatThrownBy(() -> useCase.executar(sessaoId, "oi"))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        verifyNoInteractions(assistenteIA);
    }

    @Test
    @DisplayName("sessao inexistente devolve nao encontrado")
    void sessaoInexistente() {
        when(sessaoRepository.buscarPorId(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(sessaoId, "oi"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ---------------------------------------------------------------- O-31: os citados

    /**
     * Monta o cenario da O-31: a ferramenta devolve tres produtos e o assistente responde.
     * O texto da resposta e o parametro -- e o que decide quais viram cartao.
     */
    private ChatMensagemResponse comTresProdutosERespostaDe(String resposta) {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());

        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto tinta = new Produto(UUID.randomUUID(), "SKU-TIN-001",
                "Tinta Acrílica Premium Branco Neve 18L", new BigDecimal("289.90"), 12, tintas);
        Produto rolo = new Produto(UUID.randomUUID(), "SKU-TIN-002",
                "Rolo de Lã 23cm com Cabo", new BigDecimal("34.90"), 40, tintas);
        Produto lixa = new Produto(UUID.randomUUID(), "SKU-TIN-003",
                "Lixa Grão 120", new BigDecimal("3.50"), 100, tintas);
        when(produtoRepository.buscarPorTermo(eq("tinta"), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(tinta, rolo, lixa), 0, 8, 3L, 1));

        when(assistenteIA.conversar(any(), any(), any(), any())).thenAnswer(invocacao -> {
            ExecutorDeFerramenta executor = invocacao.getArgument(3);
            executor.executar("buscar_produtos", Map.of("termos", "tinta"));
            return resposta;
        });

        return useCase.executar(sessaoId, "o que preciso para pintar?");
    }

    @Test
    @DisplayName("devolve os produtos que o assistente nomeou por extenso, e so eles")
    void devolveApenasOsCitados() {
        ChatMensagemResponse r = comTresProdutosERespostaDe(
                "Voce vai precisar de Tinta Acrílica Premium Branco Neve 18L e de Lixa Grão 120. "
                        + "Ambos ficam no corredor Tintas.");

        // A ferramenta devolveu tres; a resposta nomeou dois. So os dois viram cartao --
        // devolver os tres penduraria cartao de produto que o assistente nao recomendou.
        assertThat(r.produtosRecomendados()).extracting(ProdutoResponse::sku)
                .containsExactlyInAnyOrder("SKU-TIN-001", "SKU-TIN-003");
    }

    @Test
    @DisplayName("reconhece o produto citado pelo SKU")
    void reconhecePorSku() {
        ChatMensagemResponse r = comTresProdutosERespostaDe(
                "Leve o SKU-TIN-002, que da conta do servico.");

        assertThat(r.produtosRecomendados()).extracting(ProdutoResponse::sku)
                .containsExactly("SKU-TIN-002");
    }

    @Test
    @DisplayName("nome generico nao vira cartao: a regra da D-76 continua estrita")
    void nomeGenericoNaoViraCartao() {
        // Esta e a resposta que a O-31 mediu no ambiente publicado: ancorada e correta, mas
        // generica. Afrouxar a regra para faze-la virar cartao seria voltar ao palpite.
        ChatMensagemResponse r = comTresProdutosERespostaDe(
                "Para pintar uma parede voce precisa de tinta acrilica e lixa, "
                        + "encontradas no corredor de Tintas.");

        assertThat(r.produtosRecomendados()).isEmpty();
    }

    @Test
    @DisplayName("o acento nao separa o par: 'Lixa Grao 120' casa com 'Lixa Grão 120'")
    void acentoNaoSeparaOPar() {
        ChatMensagemResponse r = comTresProdutosERespostaDe(
                "Pegue uma Lixa Grao 120 no corredor Tintas.");

        assertThat(r.produtosRecomendados()).extracting(ProdutoResponse::sku)
                .containsExactly("SKU-TIN-003");
    }

    @Test
    @DisplayName("sem busca no catalogo, nenhum cartao -- nao ha candidato")
    void semBuscaNenhumCartao() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());
        when(assistenteIA.conversar(any(), any(), any(), any()))
                .thenReturn("Posso ajudar com o seu projeto de pintura?");

        assertThat(useCase.executar(sessaoId, "oi").produtosRecomendados()).isEmpty();
    }

    @Test
    @DisplayName("o SKU viaja para o assistente: sem ele, ele nao tem como citar sem ambiguidade")
    void aFerramentaMandaOSku() {
        comSessaoAtiva();
        aoSalvarDevolverAMensagem();
        when(chatMensagemRepository.buscarHistorico(sessaoId)).thenReturn(List.of());

        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        when(produtoRepository.buscarPorTermo(eq("tinta"), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(new Produto(UUID.randomUUID(), "SKU-TIN-001",
                        "Tinta Acrilica 18L", new BigDecimal("289.90"), 12, tintas)), 0, 8, 1L, 1));

        when(assistenteIA.conversar(any(), any(), any(), any())).thenAnswer(invocacao -> {
            ExecutorDeFerramenta executor = invocacao.getArgument(3);
            Map<String, Object> resultado = executor.executar("buscar_produtos", Map.of("termos", "tinta"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> produtos = (List<Map<String, Object>>) resultado.get("produtos");
            assertThat(produtos.get(0)).containsEntry("sku", "SKU-TIN-001");

            return "ok";
        });

        useCase.executar(sessaoId, "quero tinta");
    }
}
