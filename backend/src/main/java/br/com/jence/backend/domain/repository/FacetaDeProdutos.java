package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.AtributoProduto;

import java.util.List;

/**
 * Um filtro disponivel na tela, com os valores que existem no resultado atual e quantos
 * produtos cada um tem.
 * <p>
 * <b>As facetas sao calculadas sobre o resultado, e nao sobre o catalogo inteiro.</b> E o que
 * impede "Amperagem" de aparecer para quem esta navegando em Tintas - um filtro que nao se
 * aplica a nada e pior do que filtro nenhum, porque o cliente o experimenta e nao acontece
 * nada. Ver D-63.
 *
 * @param atributo qual caracteristica
 * @param rotulo   como ela aparece na tela, para o frontend nao precisar traduzir a chave
 * @param valores  os valores presentes, do mais comum para o menos comum
 */
public record FacetaDeProdutos(AtributoProduto atributo, String rotulo, List<Valor> valores) {

    /**
     * @param valor      o texto exibido e enviado de volta no filtro
     * @param quantidade quantos produtos do resultado atual tem este valor
     */
    public record Valor(String valor, long quantidade) {
    }
}
