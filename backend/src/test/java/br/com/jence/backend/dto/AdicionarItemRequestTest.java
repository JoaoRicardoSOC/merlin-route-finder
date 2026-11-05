package br.com.jence.backend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A validacao manual do corpo de POST /roteiro/itens, que substitui o @NotNull. */
class AdicionarItemRequestTest {

    @Test
    @DisplayName("sem produtoId, aponta o campo com a mensagem de antes")
    void semProdutoId() {
        List<StandardError.ValidationError> erros = new AdicionarItemRequest().validar();

        assertEquals(1, erros.size());
        assertEquals("produtoId", erros.get(0).getField());
        assertEquals("produtoId e obrigatorio", erros.get(0).getMessage());
    }

    @Test
    @DisplayName("com produtoId, nao ha erro")
    void comProdutoId() {
        AdicionarItemRequest requisicao = new AdicionarItemRequest();
        requisicao.setProdutoId(UUID.randomUUID());

        assertTrue(requisicao.validar().isEmpty());
    }
}
