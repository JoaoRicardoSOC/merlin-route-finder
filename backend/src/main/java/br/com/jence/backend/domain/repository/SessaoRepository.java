package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.Sessao;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessaoRepository {

    Optional<Sessao> buscarPorId(UUID id);

    /** Sessoes ainda ACTIVE cujo TTL ja venceu na data de referencia. Usado pelo cron da Fase 3. */
    List<Sessao> buscarExpiradas(LocalDateTime referencia);

    Sessao salvar(Sessao sessao);
}
