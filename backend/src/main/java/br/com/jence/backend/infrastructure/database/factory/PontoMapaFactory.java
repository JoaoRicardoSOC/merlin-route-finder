package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.infrastructure.database.entity.PontoMapaEntity;
import org.springframework.stereotype.Component;

@Component
public class PontoMapaFactory {

    public PontoMapa paraDominio(PontoMapaEntity entity) {
        if (entity == null) {
            return null;
        }
        return new PontoMapa(
                entity.getId(),
                entity.getTipo(),
                entity.getCorredor(),
                entity.getCoordenadaX(),
                entity.getCoordenadaY()
        );
    }

    public PontoMapaEntity paraPersistencia(PontoMapa pontoMapa) {
        if (pontoMapa == null) {
            return null;
        }
        return new PontoMapaEntity(
                pontoMapa.getId(),
                pontoMapa.getTipo(),
                pontoMapa.getCorredor(),
                pontoMapa.getCoordenadaX(),
                pontoMapa.getCoordenadaY()
        );
    }
}
