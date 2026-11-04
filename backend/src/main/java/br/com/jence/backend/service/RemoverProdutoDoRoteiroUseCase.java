package br.com.jence.backend.service;

import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UC-006: remove um produto da lista de roteiro.
 */
@Service
public class RemoverProdutoDoRoteiroUseCase {

    private final SessaoDao sessaoDao;
    private final ListaRoteiroDao listaRoteiroDao;

    public RemoverProdutoDoRoteiroUseCase(SessaoDao sessaoDao, ListaRoteiroDao listaRoteiroDao) {
        this.sessaoDao = sessaoDao;
        this.listaRoteiroDao = listaRoteiroDao;
    }

    public void executar(UUID sessaoId, UUID itemId)
            throws SQLException, RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        try (Transacao transacao = Transacao.iniciar()) {
            remover(sessaoId, itemId);
            transacao.confirmar();
        }
    }

    private void remover(UUID sessaoId, UUID itemId)
            throws SQLException, RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        Sessao sessao = sessaoDao.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        ListaRoteiro lista = listaRoteiroDao.buscarPorSessao(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));

        if (!lista.removerProduto(itemId)) {
            throw new RecursoNaoEncontradoException("Item do roteiro", itemId);
        }

        // A remocao vira DELETE: o DAO apaga o item lido que saiu do agregado (D-13).
        listaRoteiroDao.salvar(lista);

        sessao.renovarSessao();
        sessaoDao.salvar(sessao);
    }
}
