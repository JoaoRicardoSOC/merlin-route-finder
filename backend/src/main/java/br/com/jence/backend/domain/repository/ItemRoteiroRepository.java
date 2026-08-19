package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.ItemRoteiro;

import java.util.Optional;
import java.util.UUID;

public interface ItemRoteiroRepository {

    Optional<ItemRoteiro> buscarPorId(UUID id);

    ItemRoteiro salvar(ItemRoteiro item);
}
