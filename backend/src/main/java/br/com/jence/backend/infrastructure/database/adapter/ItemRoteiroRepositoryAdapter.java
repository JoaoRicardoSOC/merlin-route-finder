package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.repository.ItemRoteiroRepository;
import br.com.jence.backend.infrastructure.database.entity.ItemRoteiroEntity;
import br.com.jence.backend.infrastructure.database.factory.ItemRoteiroFactory;
import br.com.jence.backend.infrastructure.database.repository.ItemRoteiroJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ItemRoteiroRepositoryAdapter implements ItemRoteiroRepository {

    private final ItemRoteiroJpaRepository jpaRepository;
    private final ItemRoteiroFactory itemRoteiroFactory;

    @Override
    @Transactional(readOnly = true)
    public Optional<ItemRoteiro> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(itemRoteiroFactory::paraDominio);
    }

    /*
     * Atualiza a entidade gerenciada em vez de remapear um grafo novo: o item pertence ao
     * agregado ListaRoteiro e nao conhece a lista dona, entao recria-lo do zero gravaria
     * lista_id nula e o desligaria do roteiro.
     */
    @Override
    @Transactional
    public ItemRoteiro salvar(ItemRoteiro item) {
        ItemRoteiroEntity entity = jpaRepository.findById(item.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "ItemRoteiro %s nao encontrado. Itens novos devem ser gravados pela ListaRoteiro."
                                .formatted(item.getId())));

        entity.setColetado(item.isColetado());
        entity.setColetadoEm(item.getColetadoEm());

        return itemRoteiroFactory.paraDominio(jpaRepository.save(entity));
    }
}
