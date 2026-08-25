package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.repository.FacetaDeProdutos;

import java.util.List;

/**
 * Espelha o schema {@code Faceta}: um filtro disponivel na tela, com os valores presentes no
 * resultado atual.
 * <p>
 * O rotulo vai junto da chave para o frontend nao precisar manter a traducao de cada
 * caracteristica - se um atributo novo entrar, ele aparece na tela sozinho.
 */
public record FacetaResponse(AtributoProduto atributo, String rotulo, List<ValorResponse> valores) {

    public record ValorResponse(String valor, long quantidade) {
    }

    public static FacetaResponse de(FacetaDeProdutos faceta) {
        return new FacetaResponse(
                faceta.atributo(),
                faceta.rotulo(),
                faceta.valores().stream()
                        .map(valor -> new ValorResponse(valor.valor(), valor.quantidade()))
                        .toList());
    }
}
