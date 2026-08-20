package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Consulta o estado atual da sessao. Usado pelo Totem e pelo celular para saber se a sessao
 * ainda esta ativa antes de continuar a jornada.
 */
@Service
@RequiredArgsConstructor
public class ConsultarSessaoUseCase {

    private final SessaoRepository sessaoRepository;

    public SessaoResponse executar(UUID sessaoId) {
        return sessaoRepository.buscarPorId(sessaoId)
                .map(SessaoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));
    }
}
