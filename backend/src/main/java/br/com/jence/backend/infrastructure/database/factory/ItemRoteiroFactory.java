package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.infrastructure.database.entity.ItemRoteiroEntity;
import br.com.jence.backend.infrastructure.database.entity.ListaRoteiroEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ItemRoteiroFactory {

    private final ProdutoFactory produtoFactory;

    public ItemRoteiro paraDominio(ItemRoteiroEntity entity) {
        if (entity == null) {
            return null;
        }
        return ItemRoteiro.reconstituir(
                entity.getId(),
                produtoFactory.paraDominio(entity.getProduto()),
                entity.getColetadoEm()
        );
    }

    /*
     * Recebe a lista dona porque o lado proprietario da relacao e ItemRoteiroEntity.lista_id:
     * sem preencher esse campo, o Hibernate gravaria o item com FK nula.
     */
    public ItemRoteiroEntity paraPersistencia(ItemRoteiro item, ListaRoteiroEntity listaRoteiro) {
        if (item == null) {
            return null;
        }
        return new ItemRoteiroEntity(
                item.getId(),
                listaRoteiro,
                produtoFactory.paraPersistencia(item.getProduto()),
                item.isColetado(),
                item.getColetadoEm()
        );
    }
}
