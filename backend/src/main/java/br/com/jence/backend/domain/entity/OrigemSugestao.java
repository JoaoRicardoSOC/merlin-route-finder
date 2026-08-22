package br.com.jence.backend.domain.entity;

/**
 * De onde veio a sugestao de substituto registrada numa ruptura.
 * <p>
 * Guardado junto com o registro de proposito: quando a cota do provedor de IA estoura, o
 * sistema continua sugerindo pelo caminho deterministico, e sem esta marca seria impossivel
 * distinguir depois o que a IA de fato decidiu do que caiu no calculo de proximidade.
 */
public enum OrigemSugestao {

    /** O assistente elegeu o substituto entre os candidatos pre-filtrados. */
    ASSISTENTE_IA,

    /** Fallback: produto disponivel mais proximo, calculado por nos, sem analise semantica. */
    PROXIMIDADE,

    /** Nenhum substituto plausivel foi encontrado. */
    NENHUMA
}
