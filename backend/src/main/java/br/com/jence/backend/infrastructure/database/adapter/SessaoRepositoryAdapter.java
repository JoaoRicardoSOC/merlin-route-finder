package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.infrastructure.database.entity.SessaoEntity;
import br.com.jence.backend.infrastructure.database.factory.SessaoFactory;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class SessaoRepositoryAdapter implements SessaoRepository {

    private final SessaoJpaRepository jpaRepository;
    private final SessaoFactory sessaoFactory;

    @Override
    @Transactional(readOnly = true)
    public Optional<Sessao> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(sessaoFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Sessao> buscarExpiradas(LocalDateTime referencia) {
        return jpaRepository.findByStatusAndExpiracaoTtlBefore(StatusSessao.ACTIVE, referencia).stream()
                .map(sessaoFactory::paraDominio)
                .toList();
    }

    @Override
    @Transactional
    public Sessao salvar(Sessao sessao) {
        SessaoEntity salva = jpaRepository.save(sessaoFactory.paraPersistencia(sessao));
        return sessaoFactory.paraDominio(salva);
    }
}
