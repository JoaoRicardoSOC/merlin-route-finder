package br.com.jence.backend.dto;

/**
 * Corpo opcional de {@code POST /sessoes}: em qual placa o cliente entrou.
 */
public class IniciarSessaoRequest {

    /** Codigo de localizacao impresso na placa, em qualquer grafia. */
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
