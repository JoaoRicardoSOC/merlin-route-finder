package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UC-014 (parte 2): encerra a jornada do cliente.
 * <p>
 * Nao exige que todos os itens estejam coletados: o cliente pode desistir de um produto e ir
 * ao caixa. Tambem nao e disparado automaticamente ao coletar o ultimo item - depois de pegar
 * tudo ele ainda precisa chegar ao caixa, e encerrar ali mataria a navegacao no trecho final.
 */
@Service
@RequiredArgsConstructor
public class ConcluirRotaUseCase {

    private final SessaoRepository sessaoRepository;

    @Transactional
    public SessaoResponse executar(UUID sessaoId) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        sessao.encerrar();
        return SessaoResponse.de(sessaoRepository.salvar(sessao));
    }
}
