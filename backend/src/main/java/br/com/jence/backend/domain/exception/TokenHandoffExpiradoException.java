package br.com.jence.backend.domain.exception;

/**
 * Caso particular de token recusado: a assinatura confere, mas os 5 minutos de validade
 * passaram.
 * <p>
 * Existe separada de {@link TokenHandoffInvalidoException} porque so ela tem conserto sem
 * refazer nada: o Totem pede um QR Code novo e a jornada continua de onde estava. Adulterado,
 * malformado ou ja consumido nao se recuperam assim, e por isso continuam indistinguiveis
 * entre si.
 * <p>
 * Herda da excecao geral para que quem so quer saber "o token nao serve" continue tratando um
 * caso unico. Convertida em HTTP 401 com rotulo proprio no {@code GlobalExceptionHandler}.
 */
public class TokenHandoffExpiradoException extends TokenHandoffInvalidoException {

    public TokenHandoffExpiradoException(String motivo) {
        super(motivo);
    }
}
