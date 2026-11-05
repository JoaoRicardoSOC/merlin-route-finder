package br.com.jence.backend.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Espelha o schema {@code AdicionarItemRequest} do contrato OpenAPI. */
public class AdicionarItemRequest {

    @NotNull(message = "produtoId e obrigatorio")
    private UUID produtoId;

    public AdicionarItemRequest() {
    }

    public UUID getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(UUID produtoId) {
        this.produtoId = produtoId;
    }
}
