package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UC-004: adiciona um produto ao roteiro em montagem no Totem.
 */
@Service
@RequiredArgsConstructor
public class AdicionarProdutoAoRoteiroUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public ItemRoteiroDetalhadoResponse executar(UUID sessaoId, UUID produtoId) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        Produto produto = produtoRepository.buscarPorId(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));

        ListaRoteiro lista = listaRoteiroRepository.buscarPorSessao(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));

        // A entidade decide o que fazer com produto repetido (D-18): devolve o item existente
        // em vez de criar outro, evitando duas paradas na mesma coordenada da rota.
        ItemRoteiro item = lista.adicionarProduto(produto);
        listaRoteiroRepository.salvar(lista);

        // O cliente esta ativo montando a lista: empurra o TTL para frente. Sem isso, quem
        // monta uma lista longa (o caso que motivou remover o limite de itens) seria
        // desconectado no meio da montagem.
        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        return ItemRoteiroDetalhadoResponse.de(item);
    }
}
