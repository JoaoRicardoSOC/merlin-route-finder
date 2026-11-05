package br.com.jence.backend.dto;

import br.com.jence.backend.model.ItemRoteiro;

import java.util.UUID;
import br.com.jence.backend.factory.MapperFactory;

/**
 * Espelha o schema {@code ItemRoteiroDetalhado} do contrato OpenAPI: campos planos de
 * {@code ItemRoteiro} mais o produto aninhado, conforme o {@code allOf} do contrato.
 */
public class ItemRoteiroDetalhadoResponse {

    private UUID id;
    private UUID produtoId;
    private boolean coletado;
    private ProdutoResponse produto;

    public ItemRoteiroDetalhadoResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(UUID produtoId) {
        this.produtoId = produtoId;
    }

    public boolean isColetado() {
        return coletado;
    }

    public void setColetado(boolean coletado) {
        this.coletado = coletado;
    }

    public ProdutoResponse getProduto() {
        return produto;
    }

    public void setProduto(ProdutoResponse produto) {
        this.produto = produto;
    }

    public static ItemRoteiroDetalhadoResponse de(ItemRoteiro item) {
        ItemRoteiroDetalhadoResponse resposta =
                MapperFactory.getMapper().map(item, ItemRoteiroDetalhadoResponse.class);
        resposta.setProdutoId(item.getProduto().getId());
        resposta.setProduto(ProdutoResponse.de(item.getProduto()));
        return resposta;
    }
}
