package br.com.jence.backend.dto;

import br.com.jence.backend.factory.MapperFactory;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;

import java.util.UUID;

/** Espelha o schema {@code PontoMapa} do contrato OpenAPI. */
public class PontoMapaResponse {

    private UUID id;
    private TipoPonto tipo;
    private String corredor;
    private String codigoCurto;
    private int coordenadaX;
    private int coordenadaY;

    public PontoMapaResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TipoPonto getTipo() {
        return tipo;
    }

    public void setTipo(TipoPonto tipo) {
        this.tipo = tipo;
    }

    public String getCorredor() {
        return corredor;
    }

    public void setCorredor(String corredor) {
        this.corredor = corredor;
    }

    public String getCodigoCurto() {
        return codigoCurto;
    }

    public void setCodigoCurto(String codigoCurto) {
        this.codigoCurto = codigoCurto;
    }

    public int getCoordenadaX() {
        return coordenadaX;
    }

    public void setCoordenadaX(int coordenadaX) {
        this.coordenadaX = coordenadaX;
    }

    public int getCoordenadaY() {
        return coordenadaY;
    }

    public void setCoordenadaY(int coordenadaY) {
        this.coordenadaY = coordenadaY;
    }

    public static PontoMapaResponse de(PontoMapa pontoMapa) {
        if (pontoMapa == null) {
            return null;
        }
        return MapperFactory.getMapper().map(pontoMapa, PontoMapaResponse.class);
    }
}
