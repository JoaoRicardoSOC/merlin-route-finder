package br.com.jence.backend.domain.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Os invariantes da planta: o que, se quebrar, deixa a tela de mapa errada de um jeito que
 * ninguem percebe lendo codigo - so olhando o desenho.
 */
class PlantaDaLojaTest {

    private static final int LADO_DO_GRID = 100;

    private final List<BlocoMapa> blocos = PlantaDaLoja.blocos();

    @Test
    @DisplayName("todo bloco cabe dentro do grid")
    void dentroDoGrid() {
        // Um bloco que passa de 100 aparece cortado na borda da tela, ou fora dela.
        assertThat(blocos).allSatisfy(bloco -> {
            assertThat(bloco.x()).isBetween(0, LADO_DO_GRID);
            assertThat(bloco.y()).isBetween(0, LADO_DO_GRID);
            assertThat(bloco.x() + bloco.largura()).isLessThanOrEqualTo(LADO_DO_GRID);
            assertThat(bloco.y() + bloco.altura()).isLessThanOrEqualTo(LADO_DO_GRID);
        });
    }

    @Test
    @DisplayName("nenhum bloco se sobrepoe a outro")
    void semSobreposicao() {
        /*
         * Dois blocos sobrepostos viram corredor desenhado em cima de corredor. Pior: o
         * produto de um deles cai visualmente dentro do outro, e o cliente procura na secao
         * errada - que e exatamente o problema que o produto existe para resolver.
         */
        for (int i = 0; i < blocos.size(); i++) {
            for (int j = i + 1; j < blocos.size(); j++) {
                BlocoMapa a = blocos.get(i);
                BlocoMapa b = blocos.get(j);

                assertThat(a.sobrepoe(b))
                        .as("%s (%d,%d %dx%d) e %s (%d,%d %dx%d) se sobrepoem",
                                a.rotulo(), a.x(), a.y(), a.largura(), a.altura(),
                                b.rotulo(), b.x(), b.y(), b.largura(), b.altura())
                        .isFalse();
            }
        }
    }

    @Test
    @DisplayName("o centro de um bloco esta dentro dele")
    void centroDentro() {
        // O centro e onde a carga coloca os produtos da secao: se cair fora, a garantia que
        // motivou derivar as coordenadas da planta deixa de existir.
        assertThat(blocos).allSatisfy(bloco ->
                assertThat(bloco.contem(bloco.centroX(), bloco.centroY()))
                        .as("centro de %s", bloco.rotulo())
                        .isTrue());
    }

    @Test
    @DisplayName("nenhum rotulo se repete")
    void rotulosUnicos() {
        // O rotulo e o que liga o bloco aos pontos de prateleira daquele corredor.
        assertThat(blocos.stream().map(BlocoMapa::rotulo).toList()).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("bloco sem area e recusado na construcao")
    void dimensaoInvalida() {
        assertThatThrownBy(() -> new BlocoMapa("Vazio", 10, 10, 0, 5))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new BlocoMapa("Negativo", 10, 10, 5, -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("um bloco encostado no outro nao conta como sobreposicao")
    void encostarNaoESobrepor() {
        // Corredores lado a lado sao normais numa loja; so a area comum e problema.
        BlocoMapa esquerda = new BlocoMapa("Esquerda", 0, 0, 10, 10);
        BlocoMapa direita = new BlocoMapa("Direita", 10, 0, 10, 10);

        assertThat(esquerda.sobrepoe(direita)).isFalse();
        assertThat(direita.sobrepoe(esquerda)).isFalse();
    }
}
