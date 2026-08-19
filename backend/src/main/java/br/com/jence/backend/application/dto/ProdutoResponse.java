package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.Produto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Espelha o schema {@code Produto} do contrato OpenAPI.
 * <p>
 * Expoe apenas o {@code pontoMapaId}, nao o ponto de mapa completo: a versao com o ponto
 * aninhado e o {@code ProdutoDetalhado}, usado no endpoint de detalhamento (UC-003).
 */
public record ProdutoResponse(
        UUID id,
        String sku,
        String nome,
        BigDecimal preco,
        int saldoEstoque,
        UUID pontoMapaId
) {
    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getPreco(),
                produto.getSaldoEstoque(),
                produto.getPontoMapa() != null ? produto.getPontoMapa().getId() : null
        );
    }
}
