package br.com.jence.backend.domain.exception;

/**
 * Lancada quando o assistente de IA nao pode responder: cota esgotada, tempo esgotado, filtro
 * de conteudo do provedor ou resposta em formato inesperado.
 * <p>
 * Sinaliza a falha em vez de mascara-la, porque o fallback correto depende do contexto: na
 * ruptura de estoque e o produto disponivel mais proximo calculado por nos; no chat e uma
 * mensagem honesta de indisponibilidade. Quem decide e o caso de uso (ver D-35).
 */
public class AssistenteIAIndisponivelException extends RuntimeException {

    public AssistenteIAIndisponivelException(String motivo) {
        super(motivo);
    }

    public AssistenteIAIndisponivelException(String motivo, Throwable causa) {
        super(motivo, causa);
    }
}
