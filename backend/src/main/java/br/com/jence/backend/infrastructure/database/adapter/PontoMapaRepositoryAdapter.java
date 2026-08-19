package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.infrastructure.database.entity.PontoMapaEntity;
import br.com.jence.backend.infrastructure.database.factory.PontoMapaFactory;
import br.com.jence.backend.infrastructure.database.repository.PontoMapaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PontoMapaRepositoryAdapter implements PontoMapaRepository {

    private final PontoMapaJpaRepository jpaRepository;
    private final PontoMapaFactory pontoMapaFactory;

    @Override
    @Transactional(readOnly = true)
    public Optional<PontoMapa> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(pontoMapaFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PontoMapa> buscarPorTipo(TipoPonto tipo) {
        return jpaRepository.findByTipo(tipo).stream()
                .map(pontoMapaFactory::paraDominio)
                .toList();
    }

    @Override
    @Transactional
    public PontoMapa salvar(PontoMapa pontoMapa) {
        PontoMapaEntity salvo = jpaRepository.save(pontoMapaFactory.paraPersistencia(pontoMapa));
        return pontoMapaFactory.paraDominio(salvo);
    }
}
