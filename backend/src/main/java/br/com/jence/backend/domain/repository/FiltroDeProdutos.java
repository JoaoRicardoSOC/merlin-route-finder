package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.AtributoProduto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O que o cliente pediu na tela de catalogo. Todo campo e opcional, e ausencia significa
 * "nao filtre por isso" - nao "filtre por vazio".
 *
 * @param termo             busca por nome, tolerante a erro de digitacao. Nulo para navegar o
 *                          catalogo inteiro.
 * @param secao             corredor da loja. Nulo para todas.
 * @param apenasDisponiveis quando verdadeiro, esconde os zerados.
 * @param atributos         caracteristicas selecionadas. <b>Valores da mesma chave sao "ou",
 *                          chaves diferentes sao "e"</b>: marcar Tigre e Docol mostra os dois,
 *                          mas marcar tambem Bitola 25mm restringe aos que atendem as duas
 *                          coisas. E a semantica que todo e-commerce usa, e a que o cliente
 *                          espera sem precisar aprender.
 */
public record FiltroDeProdutos(String termo, String secao, boolean apenasDisponiveis,
                               Map<AtributoProduto, List<String>> atributos) {

    public FiltroDeProdutos {
        termo = normalizar(termo);
        secao = normalizar(secao);
        atributos = limpar(atributos);
    }

    public FiltroDeProdutos(String termo, String secao, boolean apenasDisponiveis) {
        this(termo, secao, apenasDisponiveis, Map.of());
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

    /**
     * O mesmo filtro sem as caracteristicas selecionadas.
     * <p>
     * E sobre este recorte que as facetas sao calculadas: mantendo termo, secao e
     * disponibilidade, mas ignorando os atributos, o cliente continua vendo as outras marcas
     * depois de escolher uma - e consegue trocar de ideia sem limpar o filtro primeiro.
     */
    public FiltroDeProdutos semAtributos() {
        return new FiltroDeProdutos(termo, secao, apenasDisponiveis, Map.of());
    }

    public boolean temAtributos() {
        return !atributos.isEmpty();
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

    /* Descarta chave sem valor util, que so acrescentaria um predicado que nada satisfaz. */
    private static Map<AtributoProduto, List<String>> limpar(
            Map<AtributoProduto, List<String>> selecionados) {

        if (selecionados == null || selecionados.isEmpty()) {
            return Map.of();
        }

        Map<AtributoProduto, List<String>> limpo = new LinkedHashMap<>();

        selecionados.forEach((atributo, valores) -> {
            if (atributo == null || valores == null) {
                return;
            }
            List<String> uteis = valores.stream()
                    .filter(valor -> valor != null && !valor.isBlank())
                    .map(String::trim)
                    .distinct()
                    .toList();

            if (!uteis.isEmpty()) {
                limpo.put(atributo, uteis);
            }
        });

        return Map.copyOf(limpo);
    }
}
