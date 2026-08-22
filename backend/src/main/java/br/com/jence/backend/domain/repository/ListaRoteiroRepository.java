package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.ListaRoteiro;

import java.util.Optional;
import java.util.UUID;

public interface ListaRoteiroRepository {

    Optional<ListaRoteiro> buscarPorId(UUID id);

    Optional<ListaRoteiro> buscarPorSessao(UUID sessaoId);

    /** Usado na validacao do handoff (UC-011), a partir do token lido do QR Code. */
    Optional<ListaRoteiro> buscarPorToken(String handoffToken);

    /**
     * Localiza a lista a partir de um item dela. Necessario porque o ItemRoteiro nao conhece
     * a lista que o contem, e alteracoes precisam passar pela raiz do agregado.
     */
    Optional<ListaRoteiro> buscarPorItem(UUID itemId);

    ListaRoteiro salvar(ListaRoteiro listaRoteiro);
}
