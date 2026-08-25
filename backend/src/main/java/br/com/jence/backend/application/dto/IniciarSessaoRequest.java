package br.com.jence.backend.application.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo opcional de {@code POST /sessoes}: em qual placa o cliente entrou.
 *
 * @param codigoPonto codigo de localizacao impresso na placa, em qualquer grafia
 */
public record IniciarSessaoRequest(
        @Schema(description = "Codigo de localizacao da placa, como TIN-02. "
                + "Chega aqui tanto pelo QR Code quanto digitado pelo cliente.",
                example = "TIN-02")
        String codigoPonto
) {
}
