package br.com.jence.backend.domain.exception;

/**
 * Lancada quando um recurso referenciado por identificador nao existe.
 * <p>
 * Uma unica excecao para todos os recursos, em vez de uma classe por entidade: seis classes
 * que diferem apenas no nome seriam repeticao, nao design. O contexto necessario vai na
 * mensagem.
 * <p>
 * Convertida em HTTP 404 pelo {@code GlobalExceptionHandler}.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String recurso, Object identificador) {
        super("%s nao encontrado(a): %s".formatted(recurso, identificador));
    }
}
