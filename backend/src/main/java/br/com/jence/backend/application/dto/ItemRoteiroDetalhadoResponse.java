package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ItemRoteiro;

import java.util.UUID;

/**
 * Espelha o schema {@code ItemRoteiroDetalhado} do contrato OpenAPI: campos planos de
 * {@code ItemRoteiro} mais o produto aninhado, conforme o {@code allOf} do contrato.
 */
public record ItemRoteiroDetalhadoResponse(
        UUID id,
        UUID produtoId,
        Integer ordemCaminho,
        boolean coletado,
        ProdutoResponse produto
) {
    public static ItemRoteiroDetalhadoResponse de(ItemRoteiro item) {
        return new ItemRoteiroDetalhadoResponse(
                item.getId(),
                item.getProduto().getId(),
                item.getOrdemCaminho(),
                item.isColetado(),
                ProdutoResponse.de(item.getProduto())
        );
    }
}
