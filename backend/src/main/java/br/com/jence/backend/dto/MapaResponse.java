package br.com.jence.backend.dto;

import java.util.List;

/**
 * A planta da loja pronta para desenhar.
 * <p>
 * Tudo no mesmo grid 0-100 em que vivem os produtos, as placas e a posicao do cliente - entao
 * o frontend desenha as tres camadas com a mesma escala, sem converter nada.
 */
public class MapaResponse {

    private int largura;
    private int altura;

    /** Os corredores, como retangulos com rotulo. */
    private List<BlocoMapaResponse> blocos;

    /**
     * O que nao e prateleira: caixas, banheiro e as placas de QR Code, cada um com seu tipo,
     * para o frontend escolher o que desenhar.
     */
    private List<PontoMapaResponse> pontos;

    public MapaResponse() {
    }

    public int getLargura() {
        return largura;
    }

    public void setLargura(int largura) {
        this.largura = largura;
    }

    public int getAltura() {
        return altura;
    }

    public void setAltura(int altura) {
        this.altura = altura;
    }

    public List<BlocoMapaResponse> getBlocos() {
        return blocos;
    }

    public void setBlocos(List<BlocoMapaResponse> blocos) {
        this.blocos = blocos;
    }

    public List<PontoMapaResponse> getPontos() {
        return pontos;
    }

    public void setPontos(List<PontoMapaResponse> pontos) {
        this.pontos = pontos;
    }
}
