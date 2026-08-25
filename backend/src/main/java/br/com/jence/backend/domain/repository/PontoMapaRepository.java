package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.TipoPonto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PontoMapaRepository {

    Optional<PontoMapa> buscarPorId(UUID id);

    List<PontoMapa> buscarPorTipo(TipoPonto tipo);

    /**
     * Resolve o codigo digitado pelo cliente, em qualquer grafia.
     *
     * @see PontoMapa#normalizarCodigo(String)
     */
    Optional<PontoMapa> buscarPorCodigoCurto(String codigoCurto);

    PontoMapa salvar(PontoMapa pontoMapa);
}
