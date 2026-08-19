package br.com.jence.backend.domain.exception;

/**
 * Lancada quando o recurso existe, mas seu estado atual nao permite a operacao pedida -
 * por exemplo, adicionar produto ao roteiro de uma sessao ja encerrada ou expirada.
 * <p>
 * Convertida em HTTP 409 pelo {@code GlobalExceptionHandler}. Nao confundir com
 * {@link RecursoNaoEncontradoException} (404): aqui o recurso existe.
 */
public class OperacaoNaoPermitidaException extends RuntimeException {

    public OperacaoNaoPermitidaException(String motivo) {
        super(motivo);
    }
}
