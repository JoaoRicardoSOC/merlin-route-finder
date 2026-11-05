package br.com.jence.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Corpo opcional de {@code POST /sessoes}: em qual placa o cliente entrou.
 */
public class IniciarSessaoRequest {

    /** Codigo de localizacao impresso na placa, em qualquer grafia. */
    @Schema(description = "Codigo de localizacao da placa, como TIN-02. "
            + "Chega aqui tanto pelo QR Code quanto digitado pelo cliente.",
            example = "TIN-02")
    private String codigoPonto;

    public IniciarSessaoRequest() {
    }

    public String getCodigoPonto() {
        return codigoPonto;
    }

    public void setCodigoPonto(String codigoPonto) {
        this.codigoPonto = codigoPonto;
    }
}
