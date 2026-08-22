package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.TipoPonto;
import jakarta.validation.constraints.NotNull;

/**
 * Espelha o schema {@code IncluirPontoInteresseRequest} do contrato OpenAPI, que aceita
 * apenas BANHEIRO ou CAIXA - os tipos de ponto que sao de apoio, e nao de venda.
 */
public record IncluirPontoInteresseRequest(
        @NotNull(message = "tipo e obrigatorio")
        TipoPonto tipo
) {
    public boolean isTipoDeApoio() {
        return tipo == TipoPonto.BANHEIRO || tipo == TipoPonto.CAIXA;
    }
}
