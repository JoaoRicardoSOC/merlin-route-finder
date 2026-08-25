package br.com.jence.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Corpo de {@code PUT /sessoes/{sessaoId}/posicao}: a placa onde o cliente esta agora.
 * <p>
 * Formato igual ao de {@link IniciarSessaoRequest}, significado diferente: la o codigo e
 * opcional, porque a sessao pode nascer sem posicao; aqui e o proprio objeto da operacao, e
 * uma requisicao sem ele nao quer dizer nada.
 *
 * @param codigoPonto codigo de localizacao impresso na placa, em qualquer grafia
 */
public record RecentrarSessaoRequest(
        @NotBlank(message = "informe o codigo da placa")
        @Schema(description = "Codigo de localizacao da placa onde o cliente esta agora.",
                example = "CEN-03", requiredMode = Schema.RequiredMode.REQUIRED)
        String codigoPonto
) {
}
