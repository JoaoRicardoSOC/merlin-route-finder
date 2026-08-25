package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.infrastructure.database.entity.ItemRoteiroEntity;
import br.com.jence.backend.infrastructure.database.entity.ListaRoteiroEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ListaRoteiroFactory {

    private final ItemRoteiroFactory itemRoteiroFactory;

    public ListaRoteiro paraDominio(ListaRoteiroEntity entity) {
        if (entity == null) {
            return null;
        }
        List<ItemRoteiro> itens = entity.getItens().stream()
                .map(itemRoteiroFactory::paraDominio)
                .toList();

        return ListaRoteiro.reconstituir(
                entity.getId(),
                entity.getSessaoId(),
                itens
        );
    }

    public ListaRoteiroEntity paraPersistencia(ListaRoteiro lista) {
        if (lista == null) {
            return null;
        }
        ListaRoteiroEntity entity = new ListaRoteiroEntity();
        entity.setId(lista.getId());
        entity.setSessaoId(lista.getSessaoId());

        // Os itens so podem ser mapeados depois que a entity existe, porque cada um precisa
        // apontar de volta para ela (lado proprietario da relacao).
        List<ItemRoteiroEntity> itens = new ArrayList<>();
        for (ItemRoteiro item : lista.getItens()) {
            itens.add(itemRoteiroFactory.paraPersistencia(item, entity));
        }
        entity.setItens(itens);

        return entity;
    }
}
