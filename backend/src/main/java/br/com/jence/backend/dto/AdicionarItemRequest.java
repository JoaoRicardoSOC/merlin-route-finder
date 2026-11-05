package br.com.jence.backend.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Espelha o schema {@code AdicionarItemRequest} do contrato OpenAPI. */
public class AdicionarItemRequest {

    private UUID produtoId;

    public AdicionarItemRequest() {
    }

    public UUID getProdutoId() {
        return produtoId;
    }

    public void setProdutoId(UUID produtoId) {
        this.produtoId = produtoId;
    }

    /**
     * Confere o corpo com as mesmas regras e mensagens que as anotacoes de validacao tinham.
     *
     * @return os campos com problema; vazia quando o corpo esta certo
     */
    public List<StandardError.ValidationError> validar() {
        List<StandardError.ValidationError> erros = new ArrayList<>();
        if (produtoId == null) {
            erros.add(StandardError.ValidationError.de("produtoId", "produtoId e obrigatorio"));
        }
        return erros;
    }
}
