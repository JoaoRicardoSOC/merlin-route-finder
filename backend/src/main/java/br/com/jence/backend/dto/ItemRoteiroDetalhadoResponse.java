package br.com.jence.backend.dto;

import br.com.jence.backend.model.ItemRoteiro;

import java.util.UUID;

/**
 * Espelha o schema {@code ItemRoteiroDetalhado} do contrato OpenAPI: campos planos de
 * {@code ItemRoteiro} mais o produto aninhado, conforme o {@code allOf} do contrato.
 */
public record ItemRoteiroDetalhadoResponse(
        UUID id,
        UUID produtoId,
        boolean coletado,
        ProdutoResponse produto
) {
    public static ItemRoteiroDetalhadoResponse de(ItemRoteiro item) {
        return new ItemRoteiroDetalhadoResponse(
                item.getId(),
                item.getProduto().getId(),
                item.isColetado(),
                ProdutoResponse.de(item.getProduto())
        );
    }
}
