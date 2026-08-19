package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.TipoPonto;

import java.util.UUID;

/** Espelha o schema {@code PontoMapa} do contrato OpenAPI. */
public record PontoMapaResponse(
        UUID id,
        TipoPonto tipo,
        String corredor,
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
                pontoMapa.getCoordenadaX(),
                pontoMapa.getCoordenadaY()
        );
    }
}
