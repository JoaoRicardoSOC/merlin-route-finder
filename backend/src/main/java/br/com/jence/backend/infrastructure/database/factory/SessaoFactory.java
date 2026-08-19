package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.infrastructure.database.entity.SessaoEntity;
import org.springframework.stereotype.Component;

@Component
public class SessaoFactory {

    public Sessao paraDominio(SessaoEntity entity) {
        if (entity == null) {
            return null;
        }
        return Sessao.reconstituir(
                entity.getId(),
                entity.getStatus(),
                entity.getCriadoEm(),
                entity.getExpiracaoTtl()
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
                sessao.getExpiracaoTtl()
        );
    }
}
