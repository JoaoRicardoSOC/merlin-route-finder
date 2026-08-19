package br.com.jence.backend.domain.exception;

/**
 * Lancada quando o token de handoff lido do QR Code nao pode ser aceito: assinatura
 * invalida, prazo vencido, formato incorreto ou token ja consumido (uso unico).
 * <p>
 * Convertida em HTTP 401 pelo {@code GlobalExceptionHandler}, conforme o diagrama de
 * sequencia.
 */
public class TokenHandoffInvalidoException extends RuntimeException {

    public TokenHandoffInvalidoException(String motivo) {
        super(motivo);
    }
}
