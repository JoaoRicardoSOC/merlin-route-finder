package br.com.jence.backend.domain.entity;

/**
 * Um bloco da planta da loja: o retangulo que representa um corredor na tela do mapa.
 * <p>
 * Coordenadas no mesmo grid 0-100 dos produtos e das placas, com {@code x} crescendo para a
 * direita e {@code y} para baixo. {@code x} e {@code y} sao o canto superior esquerdo.
 *
 * @param rotulo nome da secao, igual ao {@code corredor} dos pontos de prateleira
 */
public record BlocoMapa(String rotulo, int x, int y, int largura, int altura) {

    public BlocoMapa {
        if (largura <= 0 || altura <= 0) {
            throw new IllegalArgumentException(
                    "Bloco %s tem dimensao nao positiva: %dx%d".formatted(rotulo, largura, altura));
        }
    }

    /**
     * O centro do bloco, que e onde ficam os produtos da secao.
     * <p>
     * E daqui que a carga inicial tira a coordenada do ponto de prateleira: assim um produto
     * nao tem como cair fora do proprio corredor.
     */
    public int centroX() {
        return x + largura / 2;
    }

    public int centroY() {
        return y + altura / 2;
    }

    public boolean contem(int pontoX, int pontoY) {
        return pontoX >= x && pontoX <= x + largura
                && pontoY >= y && pontoY <= y + altura;
    }

    /** Dois blocos desenhados um sobre o outro viram corredor em cima de corredor na tela. */
    public boolean sobrepoe(BlocoMapa outro) {
        return x < outro.x + outro.largura && outro.x < x + largura
                && y < outro.y + outro.altura && outro.y < y + altura;
    }
}
