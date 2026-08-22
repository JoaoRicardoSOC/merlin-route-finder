package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.RegistroRuptura;

import java.util.List;
import java.util.UUID;

public interface RegistroRupturaRepository {

    RegistroRuptura salvar(RegistroRuptura registro);

    /** Rupturas relatadas numa sessao, da mais recente para a mais antiga. */
    List<RegistroRuptura> buscarPorSessao(UUID sessaoId);
}
