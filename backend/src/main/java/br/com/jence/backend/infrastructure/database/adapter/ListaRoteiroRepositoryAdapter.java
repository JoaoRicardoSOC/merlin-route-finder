package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.infrastructure.database.entity.ListaRoteiroEntity;
import br.com.jence.backend.infrastructure.database.factory.ListaRoteiroFactory;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ListaRoteiroRepositoryAdapter implements ListaRoteiroRepository {

    private final ListaRoteiroJpaRepository jpaRepository;
    private final ListaRoteiroFactory listaRoteiroFactory;

    @Override
    @Transactional(readOnly = true)
    public Optional<ListaRoteiro> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(listaRoteiroFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListaRoteiro> buscarPorSessao(UUID sessaoId) {
        return jpaRepository.findBySessaoId(sessaoId).map(listaRoteiroFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ListaRoteiro> buscarPorItem(UUID itemId) {
        return jpaRepository.findByItensId(itemId).map(listaRoteiroFactory::paraDominio);
    }

    @Override
    @Transactional
    public ListaRoteiro salvar(ListaRoteiro listaRoteiro) {
        ListaRoteiroEntity salva = jpaRepository.save(listaRoteiroFactory.paraPersistencia(listaRoteiro));
        return listaRoteiroFactory.paraDominio(salva);
    }
}
