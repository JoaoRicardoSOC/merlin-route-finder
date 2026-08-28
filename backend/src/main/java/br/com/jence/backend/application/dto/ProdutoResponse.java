package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.Produto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Espelha o schema {@code Produto} do contrato OpenAPI.
 * <p>
 * Expoe apenas o {@code pontoMapaId}, nao o ponto de mapa completo: a versao com o ponto
 * aninhado e o {@code ProdutoDetalhado}, usado no endpoint de detalhamento (UC-003).
 * <p>
 * <b>O nome do corredor vem junto, e nao so o id do ponto.</b> Sem ele a listagem obrigaria a
 * tela a cruzar cada produto com {@code GET /mapa} para descobrir onde ele fica - e enquanto
 * isso nao acontecia, o catalogo inteiro exibia um texto generico no lugar do corredor. Saber
 * em que corredor o produto esta e o que separa este sistema de um e-commerce, entao ele viaja
 * na resposta que a tela de navegacao ja consome. Ver D-71.
 */
public record ProdutoResponse(
        UUID id,
        String sku,
        String nome,
        String descricao,
        String imagemUrl,
        BigDecimal preco,
        int saldoEstoque,
        UUID pontoMapaId,

        /** Corredor onde o produto esta. Nulo se o produto nao tiver ponto no mapa. */
        String corredor
) {
    public static ProdutoResponse de(Produto produto) {
        return new ProdutoResponse(
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getImagemUrl(),
                produto.getPreco(),
                produto.getSaldoEstoque(),
                produto.getPontoMapa() != null ? produto.getPontoMapa().getId() : null,
                produto.getPontoMapa() != null ? produto.getPontoMapa().getCorredor() : null
        );
    }
}
