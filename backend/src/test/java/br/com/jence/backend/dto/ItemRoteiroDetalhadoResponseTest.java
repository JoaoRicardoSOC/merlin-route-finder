package br.com.jence.backend.dto;

import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A copia do item do roteiro. O ModelMapper casa os campos pelo nome, entao o teste confere
 * campo a campo: um campo novo com nome parecido nao pode receber o valor errado.
 */
class ItemRoteiroDetalhadoResponseTest {

    private final PontoMapa tintas =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
    private final Produto tinta = new Produto(UUID.randomUUID(), "SKU-TIN-001",
            "Tinta Acrilica Fosca Branca 18L", new BigDecimal("289.90"), 12, tintas);

    @Test
    @DisplayName("copia o item, com o id do produto e o produto aninhado")
    void copiaOsCampos() {
        UUID itemId = UUID.randomUUID();
        ItemRoteiro item = ItemRoteiro.reconstituir(itemId, tinta, LocalDateTime.now());

        ItemRoteiroDetalhadoResponse resposta = ItemRoteiroDetalhadoResponse.de(item);

        assertEquals(itemId, resposta.getId());
        assertEquals(tinta.getId(), resposta.getProdutoId());
        assertTrue(resposta.isColetado());
        assertEquals(tinta.getId(), resposta.getProduto().getId());
        assertEquals("SKU-TIN-001", resposta.getProduto().getSku());
        assertEquals("Tintas", resposta.getProduto().getCorredor());
        assertEquals(tintas.getId(), resposta.getProduto().getPontoMapaId());
    }

    @Test
    @DisplayName("item ainda nao coletado sai com coletado falso")
    void naoColetado() {
        ItemRoteiroDetalhadoResponse resposta =
                ItemRoteiroDetalhadoResponse.de(new ItemRoteiro(UUID.randomUUID(), tinta));

        assertFalse(resposta.isColetado());
    }
}
