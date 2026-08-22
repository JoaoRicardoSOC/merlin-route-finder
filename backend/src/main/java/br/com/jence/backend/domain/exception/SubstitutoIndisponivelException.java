package br.com.jence.backend.domain.exception;

/**
 * Lancada quando uma ruptura foi registrada, mas nao ha substituto plausivel a oferecer -
 * seja porque nenhum produto disponivel esta fisicamente perto, seja porque nenhum dos
 * candidatos proximos serve ao mesmo proposito.
 * <p>
 * Convertida em HTTP 422 pelo {@code GlobalExceptionHandler}: a requisicao esta correta e foi
 * processada, mas o resultado util nao existe. Nao e 404 (o item existe) nem 409 (nada no
 * estado impede a operacao).
 */
public class SubstitutoIndisponivelException extends RuntimeException {

    public SubstitutoIndisponivelException(String motivo) {
        super(motivo);
    }
}
