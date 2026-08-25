package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.infrastructure.database.entity.SessaoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessaoFactory {

    private final PontoMapaFactory pontoMapaFactory;

    public Sessao paraDominio(SessaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return Sessao.reconstituir(
                entity.getId(),
                entity.getStatus(),
                entity.getCriadoEm(),
                entity.getExpiracaoTtl(),
                pontoMapaFactory.paraDominio(entity.getPontoEscaneado()),
                entity.getEscaneadoEm()
        );
    }

    public SessaoEntity paraPersistencia(Sessao sessao) {
        if (sessao == null) {
            return null;
        }
        return new SessaoEntity(
                sessao.getId(),
                sessao.getStatus(),
                sessao.getCriadoEm(),
                sessao.getExpiracaoTtl(),
                pontoMapaFactory.paraPersistencia(sessao.getPontoEscaneado()),
                sessao.getEscaneadoEm()
        );
    }
}
