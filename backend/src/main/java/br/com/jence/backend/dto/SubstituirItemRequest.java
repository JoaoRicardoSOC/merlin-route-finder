package br.com.jence.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Corpo de {@code POST /roteiro/itens/{itemId}/substituir}: por qual produto trocar.
 * <p>
 * O id vem do cliente, e nao e deduzido pelo backend a partir da sugestao, por dois motivos.
 * O primeiro e que o assistente pode responder diferente numa segunda chamada, e a troca
 * precisa valer sobre o que o cliente <b>viu na tela</b>. O segundo e que ele nao esta preso
 * a sugestao: se encontrou outra coisa na prateleira que resolve, pode trocar por ela.
 */
public class SubstituirItemRequest {

    @Schema(description = "Produto que entra no lugar do que faltou.",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID produtoSubstitutoId;

    public SubstituirItemRequest() {
    }

    public UUID getProdutoSubstitutoId() {
        return produtoSubstitutoId;
    }

    public void setProdutoSubstitutoId(UUID produtoSubstitutoId) {
        this.produtoSubstitutoId = produtoSubstitutoId;
    }

    /**
     * Confere o corpo com as mesmas regras e mensagens que as anotacoes de validacao tinham.
     *
     * @return os campos com problema; vazia quando o corpo esta certo
     */
    public List<StandardError.ValidationError> validar() {
        List<StandardError.ValidationError> erros = new ArrayList<>();
        if (produtoSubstitutoId == null) {
            erros.add(StandardError.ValidationError.de("produtoSubstitutoId", "informe o produto substituto"));
        }
        return erros;
    }
}
