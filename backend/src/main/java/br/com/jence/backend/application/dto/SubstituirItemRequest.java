package br.com.jence.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Corpo de {@code POST /roteiro/itens/{itemId}/substituir}: por qual produto trocar.
 * <p>
 * O id vem do cliente, e nao e deduzido pelo backend a partir da sugestao, por dois motivos.
 * O primeiro e que o assistente pode responder diferente numa segunda chamada, e a troca
 * precisa valer sobre o que o cliente <b>viu na tela</b>. O segundo e que ele nao esta preso
 * a sugestao: se encontrou outra coisa na prateleira que resolve, pode trocar por ela.
 */
public record SubstituirItemRequest(
        @NotNull(message = "informe o produto substituto")
        @Schema(description = "Produto que entra no lugar do que faltou.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        UUID produtoSubstitutoId
) {
}
