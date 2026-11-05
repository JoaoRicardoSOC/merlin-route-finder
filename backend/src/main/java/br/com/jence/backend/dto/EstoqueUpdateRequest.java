package br.com.jence.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Espelha o schema {@code EstoqueUpdateRequest} do contrato OpenAPI.
 * <p>
 * Um unico campo: o saldo passa a valer o que for enviado. Zero dispara o cenario de ruptura,
 * qualquer valor positivo restaura o produto.
 */
public class EstoqueUpdateRequest {

    @NotNull(message = "saldoEstoque e obrigatorio")
    @Min(value = 0, message = "saldoEstoque nao pode ser negativo")
    private Integer saldoEstoque;

    public EstoqueUpdateRequest() {
    }

    public Integer getSaldoEstoque() {
        return saldoEstoque;
    }

    public void setSaldoEstoque(Integer saldoEstoque) {
        this.saldoEstoque = saldoEstoque;
    }
}
