package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.MensagemIA;
import br.com.jence.backend.infrastructure.ia.factory.InstrucaoDoAssistente;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
@RequiredArgsConstructor
@Slf4j
public class ConversarComAssistenteUseCase {

    /** Quantas buscas o assistente recebe por consulta, para a resposta nao virar catalogo. */
    private static final int LIMITE_DE_RESULTADOS = 8;

    private final SessaoRepository sessaoRepository;
    private final ChatMensagemRepository chatMensagemRepository;
    private final ProdutoRepository produtoRepository;
    private final AssistenteIA assistenteIA;

    @Transactional
    public ChatMensagemResponse executar(UUID sessaoId, String pergunta) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        List<MensagemIA> historico = historicoAnteriorDe(sessaoId);

        chatMensagemRepository.salvar(ChatMensagem.doCliente(UUID.randomUUID(), sessaoId, pergunta));
        historico.add(MensagemIA.doCliente(pergunta));

        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        /*
         * O que a ferramenta devolveu nesta pergunta. E o conjunto de candidatos a cartao: o
         * assistente so pode ter citado produto que ele viu. Chaveado por SKU porque a
         * ferramenta pode ser chamada mais de uma vez no mesmo turno.
         */
        Map<String, Produto> vistosPeloAssistente = new LinkedHashMap<>();

        String resposta;
        try {
            resposta = assistenteIA.conversar(
                    InstrucaoDoAssistente.instrucaoDeSistema(),
                    historico,
                    InstrucaoDoAssistente.ferramentas(),
                    (ferramenta, argumentos) -> consultarCatalogo(argumentos, vistosPeloAssistente));

        } catch (AssistenteIAIndisponivelException e) {
            /*
             * Fallback deste caso de uso (D-35): a mensagem aparece para o cliente mas nao e
             * persistida. O assistente nao deveria "lembrar" de ter dito que estava fora do ar
             * ao montar o contexto das proximas perguntas - a pergunta do cliente fica no
             * historico sem resposta, que e o que de fato aconteceu.
             */
            log.warn("Assistente indisponivel na sessao {}: {}", sessaoId, e.getMessage());
            return respostaNaoPersistida(sessaoId, InstrucaoDoAssistente.mensagemDeIndisponibilidade());
        }

        ChatMensagem salva = chatMensagemRepository.salvar(
                ChatMensagem.doAssistente(UUID.randomUUID(), sessaoId, resposta));

        return ChatMensagemResponse.de(salva, citadosEm(resposta, vistosPeloAssistente));
    }

    /**
     * Quais dos produtos vistos aparecem, por nome ou SKU, no texto da resposta.
     *
     * <p><b>A regra e estrita de proposito, e continua sendo a da D-76:</b> nome completo ou
     * SKU. Aceitar pedaco de nome traria de volta o palpite -- "tinta" casaria com qualquer
     * uma das oito. O que mudou nao foi o rigor, foi o conjunto: antes a tela comparava contra
     * o catalogo inteiro sem saber o que a IA tinha visto; aqui a lista e o que a ferramenta
     * devolveu nesta pergunta.
     *
     * <p><b>Lista vazia continua sendo resposta legitima.</b> Se o assistente responder sem
     * nomear nada por extenso, nenhum cartao aparece -- e e isso mesmo. A instrucao de sistema
     * e que pede o nome exato; a regra daqui nao afrouxa para compensar.
     */
    private List<ProdutoResponse> citadosEm(String resposta, Map<String, Produto> vistos) {
        if (resposta == null || resposta.isBlank() || vistos.isEmpty()) {
            return List.of();
        }
        String texto = comparavel(resposta);
        List<ProdutoResponse> citados = new ArrayList<>();
        for (Produto p : vistos.values()) {
            boolean porSku = p.getSku() != null && texto.contains(comparavel(p.getSku()));
            boolean porNome = p.getNome() != null && texto.contains(comparavel(p.getNome()));
            if (porSku || porNome) {
                citados.add(ProdutoResponse.de(p));
            }
        }
        log.debug("Assistente viu {} produto(s) e citou {}", vistos.size(), citados.size());
        return List.copyOf(citados);
    }

    /**
     * Texto reduzido a forma de comparacao: sem acento, sem maiuscula.
     *
     * <p>O assistente escreve "Lampada LED" onde o catalogo tem "Lâmpada LED", e comparacao
     * crua perderia o par. Normalizar resolve a classe; consertar caso a caso nao.
     */
    private static String comparavel(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private List<MensagemIA> historicoAnteriorDe(UUID sessaoId) {
        return new java.util.ArrayList<>(chatMensagemRepository.buscarHistorico(sessaoId).stream()
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
    private Map<String, Object> consultarCatalogo(Map<String, Object> argumentos,
                                                  Map<String, Produto> vistos) {
        String entrada = String.valueOf(argumentos.getOrDefault("termos", "")).trim();
        if (entrada.isBlank()) {
            return Map.of("produtos", List.of());
        }

        List<Map<String, Object>> encontrados = new ArrayList<>();

        for (String termo : entrada.split(",")) {
            String limpo = termo.trim();
            if (limpo.isEmpty()) {
                continue;
            }
            produtoRepository.buscarPorTermo(limpo, 0, LIMITE_DE_RESULTADOS).conteudo().stream()
                    // O mesmo produto pode responder a dois termos; nao repetir na resposta.
                    // O mapa serve as duas coisas: dedupe aqui, e candidatos a cartao depois.
                    .filter(p -> vistos.putIfAbsent(p.getSku(), p) == null)
                    .map(this::descrever)
                    .forEach(encontrados::add);
        }

        log.debug("Assistente buscou '{}' e recebeu {} produto(s)", entrada, encontrados.size());
        return Map.of("produtos", encontrados);
    }

    /*
     * O SKU vai junto desde a O-31. Ele e o identificador que o assistente pode escrever sem
     * ambiguidade nenhuma, e a instrucao de sistema pede que ele use o nome EXATO -- sem os
     * dois, a resposta sai com "tinta acrilica" generico e nenhum cartao aparece.
     */
    private Map<String, Object> descrever(Produto produto) {
        return Map.of(
                "sku", produto.getSku(),
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
