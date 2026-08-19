package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.ListaRoteiro;

import java.util.Optional;
import java.util.UUID;

public interface ListaRoteiroRepository {

    Optional<ListaRoteiro> buscarPorId(UUID id);

    Optional<ListaRoteiro> buscarPorSessao(UUID sessaoId);

    /** Usado na validacao do handoff (UC-011), a partir do token lido do QR Code. */
    Optional<ListaRoteiro> buscarPorToken(String handoffToken);

    ListaRoteiro salvar(ListaRoteiro listaRoteiro);
}
