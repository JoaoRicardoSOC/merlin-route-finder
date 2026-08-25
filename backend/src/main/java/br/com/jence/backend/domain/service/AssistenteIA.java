package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;

import java.util.List;
import java.util.Map;

/**
 * Assistente conversacional do sistema.
 * <p>
 * Declarado em termos de conversa e ferramentas, sem mencionar Gemini, HTTP ou JSON: o
 * dominio precisa de "alguem que responde e sabe consultar nossos dados", nao de um provedor
 * especifico: o contrato fala de negocio, a tecnologia fica na infraestrutura.
 */
public interface AssistenteIA {

    /**
     * Conversa em que o assistente pode pedir a execucao de ferramentas antes de responder.
     * <p>
     * O ciclo de ida e volta com o provedor e resolvido pela implementacao: quem chama apenas
     * informa como executar cada ferramenta.
     *
     * @param instrucaoSistema como o assistente deve se comportar
     * @param historico        a conversa ate aqui
     * @param ferramentas      o que ele pode consultar (vazio para conversa simples)
     * @param executor         executa a ferramenta pedida e devolve o resultado
     * @return o texto final da resposta
     * @throws AssistenteIAIndisponivelException em qualquer falha; o fallback e decisao de
     *                                           quem chama, nao do assistente
     */
    String conversar(String instrucaoSistema,
                     List<MensagemIA> historico,
                     List<FerramentaIA> ferramentas,
                     ExecutorDeFerramenta executor);

    /** Conversa simples, sem ferramentas. */
    default String conversar(String instrucaoSistema, List<MensagemIA> historico) {
        return conversar(instrucaoSistema, historico, List.of(), (nome, argumentos) -> Map.of());
    }
}
