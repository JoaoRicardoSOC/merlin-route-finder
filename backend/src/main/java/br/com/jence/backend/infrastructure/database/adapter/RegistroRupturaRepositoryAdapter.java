package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.RegistroRuptura;
import br.com.jence.backend.domain.repository.RegistroRupturaRepository;
import br.com.jence.backend.infrastructure.database.entity.RegistroRupturaEntity;
import br.com.jence.backend.infrastructure.database.factory.RegistroRupturaFactory;
import br.com.jence.backend.infrastructure.database.repository.RegistroRupturaJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class RegistroRupturaRepositoryAdapter implements RegistroRupturaRepository {

    private final RegistroRupturaJpaRepository jpaRepository;
    private final RegistroRupturaFactory registroRupturaFactory;

    /*
     * Transacao propria, e nao a de quem chamou. Quando a ruptura nao rende substituto, o caso
     * de uso registra o relato e so entao lanca a excecao que vira 422 - e o rollback dessa
     * excecao levaria embora exatamente o registro que documenta a falha. O relato de que a
     * gondola estava vazia e um fato independente do desfecho da requisicao. Ver D-38.
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public RegistroRuptura salvar(RegistroRuptura registro) {
        RegistroRupturaEntity salvo = jpaRepository.save(registroRupturaFactory.paraPersistencia(registro));
        return registroRupturaFactory.paraDominio(salvo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RegistroRuptura> buscarPorSessao(UUID sessaoId) {
        return jpaRepository.findBySessaoIdOrderByRegistradoEmDesc(sessaoId).stream()
                .map(registroRupturaFactory::paraDominio)
                .toList();
    }
}
