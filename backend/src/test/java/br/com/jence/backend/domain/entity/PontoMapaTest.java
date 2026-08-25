package br.com.jence.backend.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A normalizacao do codigo curto, que e regra de negocio e nao conveniencia: o codigo existe
 * para ser digitado por alguem em pe num corredor, com o adesivo rasgado ou a camera falhando.
 */
class PontoMapaTest {

    // ---------------------------------------------------------------- as grafias que o cliente digita

    @ParameterizedTest(name = "\"{0}\" leva ao mesmo ponto que TIN-02")
    @ValueSource(strings = {"TIN-02", "tin-02", "TIN02", "tin02", "TIN 02", "tin_02", "Tin-02"})
    @DisplayName("qualquer grafia plausivel chega a mesma forma canonica")
    void grafiasEquivalentes(String digitado) {
        assertThat(PontoMapa.normalizarCodigo(digitado)).isEqualTo("TIN02");
    }

    @Test
    @DisplayName("entrada sem nenhum caractere util vira nulo, e nao string vazia")
    void entradaInutilViraNulo() {
        /*
         * Devolver "" faria a consulta procurar por string vazia no banco. Como a coluna e
         * nula em todo ponto que nao e QR Code, isso nao acharia nada - mas por acidente, nao
         * por decisao. Nulo diz explicitamente "nao ha o que procurar".
         */
        assertThat(PontoMapa.normalizarCodigo(null)).isNull();
        assertThat(PontoMapa.normalizarCodigo("")).isNull();
        assertThat(PontoMapa.normalizarCodigo("   ")).isNull();
        assertThat(PontoMapa.normalizarCodigo("---")).isNull();
    }

    // ---------------------------------------------------------------- a garantia na gravacao

    @Test
    @DisplayName("o ponto guarda a forma canonica, nao a que foi passada")
    void gravaCanonico() {
        /*
         * Normalizar so na consulta deixaria o banco aceitar "TIN-02" e "tin 02" como linhas
         * diferentes, e a constraint de unicidade da coluna passaria a nao significar nada.
         */
        PontoMapa ponto = new PontoMapa(
                UUID.randomUUID(), TipoPonto.QR_CODE, "Corredor de Tintas", 32, 18, "tin-02");

        assertThat(ponto.getCodigoCurto()).isEqualTo("TIN02");
    }

    @Test
    @DisplayName("ponto que nao e QR Code nasce sem codigo")
    void semCodigo() {
        PontoMapa prateleira = new PontoMapa(
                UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

        assertThat(prateleira.getCodigoCurto()).isNull();
    }
}
