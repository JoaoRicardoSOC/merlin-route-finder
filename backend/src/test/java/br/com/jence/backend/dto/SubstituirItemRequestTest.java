package br.com.jence.backend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A validacao manual da substituicao, que substitui o @NotNull. */
class SubstituirItemRequestTest {

    @Test
    @DisplayName("sem o substituto, aponta o campo com a mensagem de antes")
    void semSubstituto() {
        List<StandardError.ValidationError> erros = new SubstituirItemRequest().validar();

        assertEquals(1, erros.size());
        assertEquals("produtoSubstitutoId", erros.get(0).getField());
        assertEquals("informe o produto substituto", erros.get(0).getMessage());
    }

    @Test
    @DisplayName("com o substituto, nao ha erro")
    void comSubstituto() {
        SubstituirItemRequest requisicao = new SubstituirItemRequest();
        requisicao.setProdutoSubstitutoId(UUID.randomUUID());

        assertTrue(requisicao.validar().isEmpty());
    }
}
