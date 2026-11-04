package br.com.jence.backend.service;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.model.*;
import br.com.jence.backend.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.model.Pagina;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.config.Configuracao;
import br.com.jence.backend.ia.GeminiClient;
import br.com.jence.backend.factory.ConexaoFalsa;
import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.ia.ExecutorDeFerramenta;
import br.com.jence.backend.model.MensagemIA;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConversarComAssistenteUseCaseTest {

    /** Sessoes em memoria. */
    static class SessaoDaoFalso extends SessaoDao {
        final Map<UUID, Sessao> sessoes = new HashMap<>();

        @Override
        public Optional<Sessao> buscarPorId(UUID id) {
            return Optional.ofNullable(sessoes.get(id));
        }

        @Override
        public Sessao salvar(Sessao sessao) {
            sessoes.put(sessao.getId(), sessao);
            return sessao;
        }
    }

    /** Historico fixo por sessao; anota cada mensagem salva e a devolve como veio. */
    static class ChatMensagemDaoFalso extends ChatMensagemDao {
        final Map<UUID, List<ChatMensagem>> historicos = new HashMap<>();
        final List<ChatMensagem> salvas = new ArrayList<>();

        @Override
        public List<ChatMensagem> buscarHistorico(UUID sessaoId) {
            return historicos.getOrDefault(sessaoId, List.of());
        }

        @Override
        public ChatMensagem salvar(ChatMensagem mensagem) {
            salvas.add(mensagem);
            return mensagem;
        }
    }

    /** Resultado combinado por termo, qualquer que seja a pagina; conta as buscas. */
    static class ProdutoDaoFalso extends ProdutoDao {
        final Map<String, Pagina<Produto>> porTermo = new HashMap<>();
        int buscas;

        @Override
        public Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho) {
            buscas++;
            return porTermo.getOrDefault(termo, new Pagina<>(List.of(), pagina, tamanho, 0L, 0));
        }
    }

    /** O que o assistente falso faz ao ser chamado. Pode usar a ferramenta, como o de verdade. */
    @FunctionalInterface
    interface Comportamento {
        String responder(List<MensagemIA> historico, ExecutorDeFerramenta executor) throws Exception;
    }

    /** Assistente que segue o comportamento combinado e anota as chamadas. */
    static class AssistenteFalso extends GeminiClient {
        Comportamento comportamento;
        int chamadas;
        List<MensagemIA> historicoRecebido;

        AssistenteFalso() {
            super(new Configuracao(Map.of()));
        }

        @Override
        public String conversar(String instrucaoSistema, List<MensagemIA> historico,
                                List<FerramentaIA> ferramentas, ExecutorDeFerramenta executor)
                throws AssistenteIAIndisponivelException {
            chamadas++;
            historicoRecebido = List.copyOf(historico);
            try {
                return comportamento.responder(historico, executor);
            } catch (RuntimeException | AssistenteIAIndisponivelException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    private SessaoDaoFalso sessaoDao;
    private ChatMensagemDaoFalso chatMensagemDao;
    private ProdutoDaoFalso produtoDao;
    private AssistenteFalso assistenteIA;
    private ConversarComAssistenteUseCase useCase;

    private UUID sessaoId;

    @BeforeEach
    void preparar() {
        ConexaoFalsa.usarNaFabrica();
        sessaoDao = new SessaoDaoFalso();
        chatMensagemDao = new ChatMensagemDaoFalso();
        produtoDao = new ProdutoDaoFalso();
        assistenteIA = new AssistenteFalso();
        useCase = new ConversarComAssistenteUseCase(sessaoDao, chatMensagemDao, produtoDao, assistenteIA);
        sessaoId = UUID.randomUUID();
    }

    @AfterEach
    void restaurar() {
        ConnectionFactory.usarBanco();
    }

    private void comSessaoAtiva() {
        sessaoDao.sessoes.put(sessaoId, Sessao.iniciar(sessaoId));
    }

    @Test
    @DisplayName("persiste a pergunta e a resposta, e devolve a resposta")
    void conversaCompleta() throws Exception {
        comSessaoAtiva();
        assistenteIA.comportamento = (historico, executor) -> "Voce vai precisar de tinta, rolo e lixa.";

        ChatMensagemResponse resposta = useCase.executar(sessaoId, "o que preciso para pintar?");

        assertEquals("Voce vai precisar de tinta, rolo e lixa.", resposta.conteudo());
        assertEquals(Remetente.ASSISTANT, resposta.remetente());

        List<ChatMensagem> salvas = chatMensagemDao.salvas;
        assertEquals(2, salvas.size());

        assertTrue(salvas.get(0).isDoCliente());
        assertEquals("o que preciso para pintar?", salvas.get(0).getConteudo());
        assertEquals(Remetente.ASSISTANT, salvas.get(1).getRemetente());
    }

    @Test
    @DisplayName("repassa o historico anterior ao assistente, na ordem, com a nova pergunta ao fim")
    void repassaHistorico() throws Exception {
        comSessaoAtiva();
        chatMensagemDao.historicos.put(sessaoId, List.of(
                ChatMensagem.doCliente(UUID.randomUUID(), sessaoId, "quero pintar o quarto"),
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, "de que cor?")));
        assistenteIA.comportamento = (historico, executor) -> "Entendi.";

        useCase.executar(sessaoId, "branco");

        assertEquals(1, assistenteIA.chamadas);
        assertEquals(List.of("quero pintar o quarto", "de que cor?", "branco"),
                assistenteIA.historicoRecebido.stream().map(MensagemIA::conteudo).toList());
    }

    /** O grounding: a ferramenta so devolve o que a nossa busca encontrou. */
    @Test
    @DisplayName("a ferramenta de busca devolve produtos reais do catalogo")
    void ferramentaConsultaOCatalogo() throws Exception {
        comSessaoAtiva();

        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto tinta = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica 18L",
                new BigDecimal("289.90"), 12, tintas);
        produtoDao.porTermo.put("tinta", new Pagina<>(List.of(tinta), 0, 8, 1L, 1));

        // simula o assistente decidindo usar a ferramenta
        assistenteIA.comportamento = (historico, executor) -> {
            Map<String, Object> resultado = executor.executar("buscar_produtos", Map.of("termos", "tinta"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> produtos = (List<Map<String, Object>>) resultado.get("produtos");
            assertEquals(1, produtos.size());
            assertEquals("Tinta Acrilica 18L", produtos.get(0).get("nome"));
            assertEquals("Tintas", produtos.get(0).get("corredor"));
            assertEquals(true, produtos.get(0).get("disponivel"));

            return "A Tinta Acrilica 18L esta no corredor Tintas.";
        };

        assertTrue(useCase.executar(sessaoId, "quero tinta").conteudo().contains("Tinta Acrilica 18L"));
    }

    /** Uma chamada da ferramenta cobre varios termos: o tier gratuito so permite 5 por minuto. */
    @Test
    @DisplayName("varios termos numa chamada so, sem repetir produto")
    void ferramentaAceitaVariosTermos() throws Exception {
        comSessaoAtiva();

        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto tinta = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica 18L",
                new BigDecimal("289.90"), 12, tintas);
        Produto rolo = new Produto(UUID.randomUUID(), "SKU-TIN-002", "Rolo de La 23cm",
                new BigDecimal("34.90"), 25, tintas);

        produtoDao.porTermo.put("tinta", new Pagina<>(List.of(tinta), 0, 8, 1L, 1));
        // "pintura" devolve os dois: a tinta ja veio antes e nao pode repetir
        produtoDao.porTermo.put("pintura", new Pagina<>(List.of(tinta, rolo), 0, 8, 2L, 1));

        assistenteIA.comportamento = (historico, executor) -> {
            Map<String, Object> resultado = executor.executar("buscar_produtos",
                    Map.of("termos", "tinta, pintura"));

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> produtos = (List<Map<String, Object>>) resultado.get("produtos");
            assertEquals(2, produtos.size());
            assertEquals(List.of("Tinta Acrilica 18L", "Rolo de La 23cm"),
                    produtos.stream().map(p -> p.get("nome")).toList());
            return "ok";
        };

        useCase.executar(sessaoId, "quero pintar");

        // uma unica ida ao assistente, apesar dos dois termos
        assertEquals(1, assistenteIA.chamadas);
    }

    @Test
    @DisplayName("termo vazio na ferramenta nao consulta o banco")
    void ferramentaComTermoVazio() throws Exception {
        comSessaoAtiva();
        assistenteIA.comportamento = (historico, executor) -> {
            assertEquals(List.of(), executor.executar("buscar_produtos", Map.of()).get("produtos"));
            return "ok";
        };

        useCase.executar(sessaoId, "oi");

        assertEquals(0, produtoDao.buscas);
    }

    /** Aplicacao da D-35: o fallback e daqui, e a falha nao vira registro. */
    @Test
    @DisplayName("IA indisponivel devolve mensagem amigavel e nao persiste a falha")
    void iaIndisponivel() throws Exception {
        comSessaoAtiva();
        assistenteIA.comportamento = (historico, executor) -> {
            throw new AssistenteIAIndisponivelException("cota esgotada");
        };

        ChatMensagemResponse resposta = useCase.executar(sessaoId, "o que preciso para pintar?");

        assertTrue(resposta.conteudo().contains("Não consegui consultar o assistente"));

        // so a pergunta do cliente foi salva: a falha nao entra no historico
        assertEquals(1, chatMensagemDao.salvas.size());
    }

    @Test
    @DisplayName("sessao inativa e recusada")
    void sessaoInativa() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        sessaoDao.sessoes.put(sessaoId, encerrada);

        assertThrows(OperacaoNaoPermitidaException.class, () -> useCase.executar(sessaoId, "oi"));

        assertEquals(0, assistenteIA.chamadas);
    }

    @Test
    @DisplayName("sessao inexistente devolve nao encontrado")
    void sessaoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.executar(sessaoId, "oi"));
    }
}
