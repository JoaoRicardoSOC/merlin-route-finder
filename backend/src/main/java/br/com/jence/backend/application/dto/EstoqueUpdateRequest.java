package br.com.jence.backend.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Espelha o schema {@code EstoqueUpdateRequest} do contrato OpenAPI.
 * <p>
 * Um unico campo: o saldo passa a valer o que for enviado. Zero dispara o cenario de ruptura,
 * qualquer valor positivo restaura o produto.
 */
public record EstoqueUpdateRequest(
        @NotNull(message = "saldoEstoque e obrigatorio")
        @Min(value = 0, message = "saldoEstoque nao pode ser negativo")
        Integer saldoEstoque
) {
}
