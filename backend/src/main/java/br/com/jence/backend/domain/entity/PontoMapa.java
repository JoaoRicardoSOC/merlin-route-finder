package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class PontoMapa {

    private final UUID id;
    private final TipoPonto tipo;
    private final String corredor;
    private final int coordenadaX;
    private final int coordenadaY;

    public PontoMapa(UUID id, TipoPonto tipo, String corredor, int coordenadaX, int coordenadaY) {
        this.id = id;
        this.tipo = tipo;
        this.corredor = corredor;
        this.coordenadaX = coordenadaX;
        this.coordenadaY = coordenadaY;
    }

    public double calcularDistanciaPara(PontoMapa outro) {
        int deltaX = this.coordenadaX - outro.coordenadaX;
        int deltaY = this.coordenadaY - outro.coordenadaY;
        return Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
    }
}
