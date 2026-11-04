package br.com.jence.backend.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ProdutoTest {

    private static final PontoMapa TINTAS = new PontoMapa(
            UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

    @Test
    @DisplayName("mudar o saldo nao perde a apresentacao")
    void saldoPreservaApresentacao() {
        /*
         * A ferramenta de simulacao da demonstracao zera e restaura estoque ao vivo. Se a copia
         * perdesse descricao e imagem, o produto ficaria sem foto no meio da apresentacao -
         * justamente no cenario de ruptura, que e o momento mais forte da demo.
         */
        Produto completo = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                "Tinta de acabamento fosco para paredes internas e externas.",
                "https://exemplo.leroymerlin.com.br/tinta.jpg",
                new BigDecimal("289.90"), 12, TINTAS);

        Produto zerado = completo.comSaldoEstoque(0);

        assertEquals(completo.getNome(), zerado.getNome());
        assertEquals(completo.getDescricao(), zerado.getDescricao());
        assertEquals(completo.getImagemUrl(), zerado.getImagemUrl());
        assertFalse(zerado.temDisponibilidade());
    }
}
