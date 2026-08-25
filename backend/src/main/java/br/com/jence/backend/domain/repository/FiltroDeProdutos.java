package br.com.jence.backend.domain.repository;

/**
 * O que o cliente pediu na tela de catalogo. Todo campo e opcional, e ausencia significa
 * "nao filtre por isso" - nao "filtre por vazio".
 *
 * @param termo              busca por nome, tolerante a erro de digitacao. Nulo para navegar
 *                           o catalogo inteiro.
 * @param secao              corredor da loja. Nulo para todas.
 * @param apenasDisponiveis  quando verdadeiro, esconde os zerados.
 */
public record FiltroDeProdutos(String termo, String secao, boolean apenasDisponiveis) {

    public FiltroDeProdutos {
        termo = normalizar(termo);
        secao = normalizar(secao);
    }

    /** Sem filtro nenhum: o catalogo inteiro, como o cliente o ve ao abrir a tela. */
    public static FiltroDeProdutos nenhum() {
        return new FiltroDeProdutos(null, null, false);
    }

    public static FiltroDeProdutos porTermo(String termo) {
        return new FiltroDeProdutos(termo, null, false);
    }

    public boolean temTermo() {
        return termo != null;
    }

    /*
     * Texto em branco vira nulo aqui, e nao na consulta, por um motivo do Oracle: la a string
     * vazia E nula, entao usar "" como sentinela de "sem filtro" funcionaria por acidente e
     * quebraria em qualquer outro banco. Resolvido no dominio, o comportamento fica explicito.
     */
    private static String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }
}
