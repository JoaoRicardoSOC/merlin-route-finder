package br.com.jence.backend.dto;

import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.model.ValorDeAtributo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A copia do produto para o detalhe. O ModelMapper casa os campos pelo nome, entao o teste
 * confere campo a campo: um campo novo com nome parecido nao pode receber o valor errado.
 */
class ProdutoDetalhadoResponseTest {

    @Test
    @DisplayName("copia cada campo do produto, com o ponto aninhado e as especificacoes")
    void copiaOsCampos() {
        PontoMapa tintas = new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto produto = new Produto(UUID.randomUUID(), "SKU-TIN-001",
                "Tinta Acrilica Fosca Branca 18L", "Tinta de acabamento fosco.",
                "https://exemplo.com/tinta.png", new BigDecimal("289.90"), 12, tintas);

        ProdutoDetalhadoResponse resposta = ProdutoDetalhadoResponse.de(produto,
                List.of(new ValorDeAtributo(AtributoProduto.MARCA, "Suvinil")));

        assertEquals(produto.getId(), resposta.getId());
        assertEquals("SKU-TIN-001", resposta.getSku());
        assertEquals("Tinta Acrilica Fosca Branca 18L", resposta.getNome());
        assertEquals("Tinta de acabamento fosco.", resposta.getDescricao());
        assertEquals("https://exemplo.com/tinta.png", resposta.getImagemUrl());
        assertEquals(new BigDecimal("289.90"), resposta.getPreco());
        assertEquals(12, resposta.getSaldoEstoque());
        assertEquals(tintas.getId(), resposta.getPontoMapaId());
        assertEquals("Tintas", resposta.getCorredor());
        assertEquals(tintas.getId(), resposta.getPontoMapa().getId());
        assertEquals(32, resposta.getPontoMapa().getCoordenadaX());

        assertEquals(1, resposta.getAtributos().size());
        AtributoResponse marca = resposta.getAtributos().get(0);
        assertEquals(AtributoProduto.MARCA, marca.getAtributo());
        assertEquals("Marca", marca.getRotulo());
        assertEquals("Suvinil", marca.getValor());
    }

    @Test
    @DisplayName("produto sem ponto no mapa fica sem ponto, id e corredor")
    void semPonto() {
        Produto produto = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta",
                new BigDecimal("289.90"), 12, null);

        ProdutoDetalhadoResponse resposta = ProdutoDetalhadoResponse.de(produto);

        assertNull(resposta.getPontoMapa());
        assertNull(resposta.getPontoMapaId());
        assertNull(resposta.getCorredor());
        assertTrue(resposta.getAtributos().isEmpty());
    }
}
