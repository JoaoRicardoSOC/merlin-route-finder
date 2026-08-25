package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Espelha o schema {@code ProdutoDetalhado} do contrato OpenAPI.
 * <p>
 * Os campos sao repetidos em vez de compostos a partir de {@link ProdutoResponse} porque o
 * contrato usa {@code allOf}, que resulta num objeto JSON plano. Compor geraria um
 * {@code produto} aninhado, divergindo do contrato.
 */
public record ProdutoDetalhadoResponse(
        UUID id,
        String sku,
        String nome,
        String descricao,
        String imagemUrl,
        BigDecimal preco,
        int saldoEstoque,
        UUID pontoMapaId,
        PontoMapaResponse pontoMapa,
        List<AtributoResponse> atributos
) {
    /** Detalhe sem especificacoes: usado onde o produto aparece embutido em outra resposta. */
    public static ProdutoDetalhadoResponse de(Produto produto) {
        return de(produto, List.of());
    }

    public static ProdutoDetalhadoResponse de(Produto produto, List<ValorDeAtributo> atributos) {
        return new ProdutoDetalhadoResponse(
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getImagemUrl(),
                produto.getPreco(),
                produto.getSaldoEstoque(),
                produto.getPontoMapa() != null ? produto.getPontoMapa().getId() : null,
                PontoMapaResponse.de(produto.getPontoMapa()),
                atributos.stream().map(AtributoResponse::de).toList()
        );
    }
}
