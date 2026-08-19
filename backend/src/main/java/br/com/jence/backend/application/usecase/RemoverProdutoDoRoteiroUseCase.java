package br.com.jence.backend.application.usecase;

import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UC-006: remove um produto da lista de roteiro.
 */
@Service
@RequiredArgsConstructor
public class RemoverProdutoDoRoteiroUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;

    @Transactional
    public void executar(UUID sessaoId, UUID itemId) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        ListaRoteiro lista = listaRoteiroRepository.buscarPorSessao(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));

        if (!lista.removerProduto(itemId)) {
            throw new RecursoNaoEncontradoException("Item do roteiro", itemId);
        }

        // A remocao vira DELETE via orphanRemoval do agregado (D-13).
        listaRoteiroRepository.salvar(lista);

        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);
    }
}
