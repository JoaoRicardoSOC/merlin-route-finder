package br.com.jence.backend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A validacao manual do ajuste de estoque, que substitui o @NotNull e o @Min. */
class EstoqueUpdateRequestTest {

    private List<StandardError.ValidationError> validar(Integer saldo) {
        EstoqueUpdateRequest requisicao = new EstoqueUpdateRequest();
        requisicao.setSaldoEstoque(saldo);
        return requisicao.validar();
    }

    @Test
    @DisplayName("sem saldo, aponta o campo como obrigatorio")
    void semSaldo() {
        List<StandardError.ValidationError> erros = validar(null);

        assertEquals(1, erros.size());
        assertEquals("saldoEstoque", erros.get(0).getField());
        assertEquals("saldoEstoque e obrigatorio", erros.get(0).getMessage());
    }

    @Test
    @DisplayName("saldo negativo e recusado")
    void saldoNegativo() {
        List<StandardError.ValidationError> erros = validar(-1);

        assertEquals(1, erros.size());
        assertEquals("saldoEstoque nao pode ser negativo", erros.get(0).getMessage());
    }

    @Test
    @DisplayName("zero e permitido, porque e o que dispara a ruptura")
    void zeroPassa() {
        assertTrue(validar(0).isEmpty());
        assertTrue(validar(25).isEmpty());
    }
}
