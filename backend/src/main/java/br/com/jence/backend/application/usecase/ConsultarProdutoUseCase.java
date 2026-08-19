package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * UC-003: detalhamento de um produto, incluindo onde ele esta fisicamente na loja.
 * <p>
 * O saldo de estoque e lido da nossa propria tabela no momento da consulta, sem cache. Nao
 * ha integracao com ERP/WMS no escopo do projeto: o dado e tao atual quanto o que estiver
 * gravado no banco.
 */
@Service
@RequiredArgsConstructor
public class ConsultarProdutoUseCase {

    private final ProdutoRepository produtoRepository;

    public ProdutoDetalhadoResponse executar(UUID produtoId) {
        return produtoRepository.buscarPorId(produtoId)
                .map(ProdutoDetalhadoResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));
    }
}
