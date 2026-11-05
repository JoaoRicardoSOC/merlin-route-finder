package br.com.jence.backend.dto;

import br.com.jence.backend.model.BlocoMapa;

/** Espelha o schema {@code BlocoMapa} do contrato: um retangulo a desenhar na tela do mapa. */
public class BlocoMapaResponse {

    private String rotulo;
    private int x;
    private int y;
    private int largura;
    private int altura;

    public BlocoMapaResponse() {
    }

    public String getRotulo() {
        return rotulo;
    }

    public void setRotulo(String rotulo) {
        this.rotulo = rotulo;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
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

    public static BlocoMapaResponse de(BlocoMapa bloco) {
        BlocoMapaResponse resposta = new BlocoMapaResponse();
        resposta.setRotulo(bloco.rotulo());
        resposta.setX(bloco.x());
        resposta.setY(bloco.y());
        resposta.setLargura(bloco.largura());
        resposta.setAltura(bloco.altura());
        return resposta;
    }
}
