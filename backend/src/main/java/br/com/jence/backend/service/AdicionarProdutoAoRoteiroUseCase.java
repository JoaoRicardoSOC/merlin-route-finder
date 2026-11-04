package br.com.jence.backend.service;

import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UC-004: adiciona um produto ao roteiro em montagem no Totem.
 */
@Service
public class AdicionarProdutoAoRoteiroUseCase {

    private final SessaoDao sessaoDao;
    private final ListaRoteiroDao listaRoteiroDao;
    private final ProdutoDao produtoDao;

    public AdicionarProdutoAoRoteiroUseCase(SessaoDao sessaoDao,
                                            ListaRoteiroDao listaRoteiroDao,
                                            ProdutoDao produtoDao) {
        this.sessaoDao = sessaoDao;
        this.listaRoteiroDao = listaRoteiroDao;
        this.produtoDao = produtoDao;
    }

    public ItemRoteiroDetalhadoResponse executar(UUID sessaoId, UUID produtoId) throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            ItemRoteiroDetalhadoResponse resposta = adicionar(sessaoId, produtoId);
            transacao.confirmar();
            return resposta;
        }
    }

    private ItemRoteiroDetalhadoResponse adicionar(UUID sessaoId, UUID produtoId) throws SQLException {
        Sessao sessao = sessaoDao.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        Produto produto = produtoDao.buscarPorId(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));

        ListaRoteiro lista = listaRoteiroDao.buscarPorSessao(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));

        // A entidade decide o que fazer com produto repetido (D-18): devolve o item existente
        // em vez de criar outro, evitando duas paradas na mesma coordenada da rota.
        ItemRoteiro item = lista.adicionarProduto(produto);
        listaRoteiroDao.salvar(lista);

        // O cliente esta ativo montando a lista: empurra o TTL para frente. Sem isso, quem
        // monta uma lista longa (o caso que motivou remover o limite de itens) seria
        // desconectado no meio da montagem.
        sessao.renovarSessao();
        sessaoDao.salvar(sessao);

        return ItemRoteiroDetalhadoResponse.de(item);
    }
}
