package br.com.jence.backend.dto;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;

import java.util.UUID;

/** Espelha o schema {@code PontoMapa} do contrato OpenAPI. */
public record PontoMapaResponse(
        UUID id,
        TipoPonto tipo,
        String corredor,
        String codigoCurto,
        int coordenadaX,
        int coordenadaY
) {
    public static PontoMapaResponse de(PontoMapa pontoMapa) {
        if (pontoMapa == null) {
            return null;
        }
        return new PontoMapaResponse(
                pontoMapa.getId(),
                pontoMapa.getTipo(),
                pontoMapa.getCorredor(),
                pontoMapa.getCodigoCurto(),
                pontoMapa.getCoordenadaX(),
                pontoMapa.getCoordenadaY()
        );
    }
}
