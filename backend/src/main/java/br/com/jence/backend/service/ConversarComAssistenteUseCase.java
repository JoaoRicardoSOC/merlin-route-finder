package br.com.jence.backend.service;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.ia.GeminiClient;
import br.com.jence.backend.model.MensagemIA;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.ia.InstrucaoDoAssistente;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * UC-007 a UC-009: o cliente pergunta o que precisa para o projeto dele e o assistente
 * responde citando produtos reais da loja.
 * <p>
 * Ataca a assimetria de informacao descrita no desafio: o conhecimento tecnico deixa de estar
 * so com o vendedor.
 */
@Service
public class ConversarComAssistenteUseCase {

    /** Quantas buscas o assistente recebe por consulta, para a resposta nao virar catalogo. */
    private static final int LIMITE_DE_RESULTADOS = 8;

    private final SessaoDao sessaoDao;
    private final ChatMensagemDao chatMensagemDao;
    private final ProdutoDao produtoDao;
    private final GeminiClient assistenteIA;

    public ConversarComAssistenteUseCase(SessaoDao sessaoDao,
                                         ChatMensagemDao chatMensagemDao,
                                         ProdutoDao produtoDao,
                                         GeminiClient assistenteIA) {
        this.sessaoDao = sessaoDao;
        this.chatMensagemDao = chatMensagemDao;
        this.produtoDao = produtoDao;
        this.assistenteIA = assistenteIA;
    }

    public ChatMensagemResponse executar(UUID sessaoId, String pergunta) throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            ChatMensagemResponse resposta = conversar(sessaoId, pergunta);
            transacao.confirmar();
            return resposta;
        }
    }

    private ChatMensagemResponse conversar(UUID sessaoId, String pergunta) throws SQLException {
        Sessao sessao = sessaoDao.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        List<MensagemIA> historico = historicoAnteriorDe(sessaoId);

        chatMensagemDao.salvar(ChatMensagem.doCliente(UUID.randomUUID(), sessaoId, pergunta));
        historico.add(MensagemIA.doCliente(pergunta));

        sessao.renovarSessao();
        sessaoDao.salvar(sessao);

        String resposta;
        try {
            resposta = assistenteIA.conversar(
                    InstrucaoDoAssistente.instrucaoDeSistema(),
                    historico,
                    InstrucaoDoAssistente.ferramentas(),
                    this::consultarCatalogo);

        } catch (AssistenteIAIndisponivelException e) {
            /*
             * Fallback deste caso de uso (D-35): a mensagem aparece para o cliente mas nao e
             * persistida. O assistente nao deveria "lembrar" de ter dito que estava fora do ar
             * ao montar o contexto das proximas perguntas - a pergunta do cliente fica no
             * historico sem resposta, que e o que de fato aconteceu.
             */
            System.out.println(String.format("AVISO: Assistente indisponivel na sessao %s: %s", sessaoId, e.getMessage()));
            return respostaNaoPersistida(sessaoId, InstrucaoDoAssistente.mensagemDeIndisponibilidade());
        }

        ChatMensagem salva = chatMensagemDao.salvar(
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, resposta));

        return ChatMensagemResponse.de(salva);
    }

    private List<MensagemIA> historicoAnteriorDe(UUID sessaoId) throws SQLException {
        return new java.util.ArrayList<>(chatMensagemDao.buscarHistorico(sessaoId).stream()
                .map(m -> m.isDoCliente()
                        ? MensagemIA.doCliente(m.getConteudo())
                        : MensagemIA.doAssistente(m.getConteudo()))
                .toList());
    }

    /*
     * A ferramenta que o assistente consulta. E aqui que o grounding acontece: ele so consegue
     * falar de produtos que esta busca devolveu. Reaproveita a busca tolerante a erro de
     * digitacao do UC-002.
     *
     * Aceita varios termos numa chamada porque o tier gratuito do Gemini limita a 5
     * requisicoes por minuto: buscar produto a produto esgotaria a cota numa unica pergunta.
     */
    private Map<String, Object> consultarCatalogo(String ferramenta, Map<String, Object> argumentos)
            throws SQLException {
        String entrada = String.valueOf(argumentos.getOrDefault("termos", "")).trim();
        if (entrada.isBlank()) {
            return Map.of("produtos", List.of());
        }

        List<Map<String, Object>> encontrados = new java.util.ArrayList<>();
        java.util.Set<String> jaIncluidos = new java.util.HashSet<>();

        for (String termo : entrada.split(",")) {
            String limpo = termo.trim();
            if (limpo.isEmpty()) {
                continue;
            }
            produtoDao.buscarPorTermo(limpo, 0, LIMITE_DE_RESULTADOS).conteudo().stream()
                    // O mesmo produto pode responder a dois termos; nao repetir na resposta.
                    .filter(p -> jaIncluidos.add(p.getSku()))
                    .map(this::descrever)
                    .forEach(encontrados::add);
        }

        return Map.of("produtos", encontrados);
    }

    private Map<String, Object> descrever(Produto produto) {
        return Map.of(
                "nome", produto.getNome(),
                "preco", produto.getPreco().toString(),
                "disponivel", produto.temDisponibilidade(),
                "corredor", produto.getPontoMapa() != null ? produto.getPontoMapa().getCorredor() : "nao informado");
    }

    private ChatMensagemResponse respostaNaoPersistida(UUID sessaoId, String conteudo) {
        return ChatMensagemResponse.de(
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, conteudo));
    }
}
