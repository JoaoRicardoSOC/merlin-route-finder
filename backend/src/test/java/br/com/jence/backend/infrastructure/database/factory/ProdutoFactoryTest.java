package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(voltou.getSku()).isEqualTo("SKU-TIN-001");
        assertThat(voltou.getNome()).isEqualTo("Tinta Acrilica Fosca Branca 18L");
        assertThat(voltou.getDescricao())
                .isEqualTo("Tinta de acabamento fosco para paredes internas e externas.");
        assertThat(voltou.getImagemUrl()).isEqualTo("https://exemplo.leroymerlin.com.br/tinta.jpg");
        assertThat(voltou.getPreco()).isEqualByComparingTo("289.90");
        assertThat(voltou.getSaldoEstoque()).isEqualTo(12);
        assertThat(voltou.getPontoMapa().getCorredor()).isEqualTo("Tintas");
    }

    @Test
    @DisplayName("a entidade recebe cada valor na coluna certa")
    void colunasCertas() {
        // Uma troca simetrica entre duas colunas passaria despercebida por um teste que so
        // olha a ida e a volta.
        ProdutoEntity entity = factory.paraPersistencia(completo());

        assertThat(entity.getNome()).isEqualTo("Tinta Acrilica Fosca Branca 18L");
        assertThat(entity.getDescricao())
                .isEqualTo("Tinta de acabamento fosco para paredes internas e externas.");
        assertThat(entity.getImagemUrl()).isEqualTo("https://exemplo.leroymerlin.com.br/tinta.jpg");
    }

    @Test
    @DisplayName("produto sem descricao nem imagem atravessa a traducao como nulo")
    void semApresentacao() {
        Produto cru = new Produto(ID, "SKU-X", "Item", new BigDecimal("1.00"), 1, TINTAS);

        Produto voltou = factory.paraDominio(factory.paraPersistencia(cru));

        assertThat(voltou.getDescricao()).isNull();
        assertThat(voltou.getImagemUrl()).isNull();
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

        assertThat(zerado.getDescricao()).isNotNull();
        assertThat(zerado.getImagemUrl()).isNotNull();
        assertThat(zerado.temDisponibilidade()).isFalse();
    }
}
