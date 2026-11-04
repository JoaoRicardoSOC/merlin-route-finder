package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * A traducao do produto entre dominio e persistencia, campo a campo.
 * <p>
 * Existe pela mesma razao do teste equivalente de ponto de mapa: {@code ProdutoEntity} usa
 * {@code @AllArgsConstructor}, que segue a ordem de declaracao dos campos. <b>Nome, descricao
 * e imagem sao tres String vizinhas</b> - trocar duas delas nao daria erro de compilacao
 * nenhum, e o catalogo apareceria com a descricao no lugar do nome.
 */
class ProdutoFactoryTest {

    private final ProdutoFactory factory = new ProdutoFactory(new PontoMapaFactory());

    private static final UUID ID = UUID.randomUUID();
    private static final PontoMapa TINTAS = new PontoMapa(
            UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

    private Produto completo() {
        return new Produto(ID, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                "Tinta de acabamento fosco para paredes internas e externas.",
                "https://exemplo.leroymerlin.com.br/tinta.jpg",
                new BigDecimal("289.90"), 12, TINTAS);
    }

    @Test
    @DisplayName("ida e volta preserva cada campo no seu lugar")
    void idaEVolta() {
        Produto voltou = factory.paraDominio(factory.paraPersistencia(completo()));

        assertEquals("SKU-TIN-001", voltou.getSku());
        assertEquals("Tinta Acrilica Fosca Branca 18L", voltou.getNome());
        assertEquals("Tinta de acabamento fosco para paredes internas e externas.",
                voltou.getDescricao());
        assertEquals("https://exemplo.leroymerlin.com.br/tinta.jpg", voltou.getImagemUrl());
        assertEquals(0, voltou.getPreco().compareTo(new BigDecimal("289.90")));
        assertEquals(12, voltou.getSaldoEstoque());
        assertEquals("Tintas", voltou.getPontoMapa().getCorredor());
    }

    @Test
    @DisplayName("a entidade recebe cada valor na coluna certa")
    void colunasCertas() {
        // Uma troca simetrica entre duas colunas passaria despercebida por um teste que so
        // olha a ida e a volta.
        ProdutoEntity entity = factory.paraPersistencia(completo());

        assertEquals("Tinta Acrilica Fosca Branca 18L", entity.getNome());
        assertEquals("Tinta de acabamento fosco para paredes internas e externas.",
                entity.getDescricao());
        assertEquals("https://exemplo.leroymerlin.com.br/tinta.jpg", entity.getImagemUrl());
    }

    @Test
    @DisplayName("produto sem descricao nem imagem atravessa a traducao como nulo")
    void semApresentacao() {
        Produto cru = new Produto(ID, "SKU-X", "Item", new BigDecimal("1.00"), 1, TINTAS);

        Produto voltou = factory.paraDominio(factory.paraPersistencia(cru));

        assertNull(voltou.getDescricao());
        assertNull(voltou.getImagemUrl());
    }

    @Test
    @DisplayName("mudar o saldo nao perde a apresentacao")
    void saldoPreservaApresentacao() {
        /*
         * A ferramenta de simulacao da demonstracao zera e restaura estoque ao vivo. Se a copia
         * perdesse descricao e imagem, o produto ficaria sem foto no meio da apresentacao -
         * justamente no cenario de ruptura, que e o momento mais forte da demo.
         */
        Produto zerado = completo().comSaldoEstoque(0);

        assertNotNull(zerado.getDescricao());
        assertNotNull(zerado.getImagemUrl());
        assertFalse(zerado.temDisponibilidade());
    }
}
