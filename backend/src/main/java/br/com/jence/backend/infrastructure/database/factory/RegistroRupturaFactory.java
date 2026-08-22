package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.RegistroRuptura;
import br.com.jence.backend.infrastructure.database.entity.RegistroRupturaEntity;
import org.springframework.stereotype.Component;

@Component
public class RegistroRupturaFactory {

    public RegistroRuptura paraDominio(RegistroRupturaEntity entity) {
        if (entity == null) {
            return null;
        }
        return RegistroRuptura.reconstituir(
                entity.getId(),
                entity.getSessaoId(),
                entity.getItemRoteiroId(),
                entity.getProdutoFaltanteId(),
                entity.getProdutoSugeridoId(),
                entity.getJustificativa(),
                entity.getOrigem(),
                entity.getRegistradoEm()
        );
    }

    public RegistroRupturaEntity paraPersistencia(RegistroRuptura registro) {
        if (registro == null) {
            return null;
        }
        RegistroRupturaEntity entity = new RegistroRupturaEntity();
        entity.setId(registro.getId());
        entity.setSessaoId(registro.getSessaoId());
        entity.setItemRoteiroId(registro.getItemRoteiroId());
        entity.setProdutoFaltanteId(registro.getProdutoFaltanteId());
        entity.setProdutoSugeridoId(registro.getProdutoSugeridoId());
        entity.setJustificativa(registro.getJustificativa());
        entity.setOrigem(registro.getOrigem());
        entity.setRegistradoEm(registro.getRegistradoEm());
        return entity;
    }
}
