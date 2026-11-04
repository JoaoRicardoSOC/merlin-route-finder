package br.com.jence.backend.service;

import br.com.jence.backend.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UC-003: detalhamento de um produto, incluindo onde ele esta fisicamente na loja.
 * <p>
 * O saldo de estoque e lido da nossa propria tabela no momento da consulta, sem cache. Nao
 * ha integracao com ERP/WMS no escopo do projeto: o dado e tao atual quanto o que estiver
 * gravado no banco.
 * <p>
 * E a unica tela que traz as caracteristicas do produto - marca, medidas, material -, que sao
 * o que o cliente compara antes de decidir.
 */
@Service
public class ConsultarProdutoUseCase {

    private final ProdutoDao produtoDao;

    public ConsultarProdutoUseCase(ProdutoDao produtoDao) {
        this.produtoDao = produtoDao;
    }

    public ProdutoDetalhadoResponse executar(UUID produtoId) throws SQLException {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            ProdutoDetalhadoResponse resposta = consultar(produtoId);
            transacao.confirmar();
            return resposta;
        }
    }

    private ProdutoDetalhadoResponse consultar(UUID produtoId) throws SQLException {
        Produto produto = produtoDao.buscarPorId(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));

        /*
         * Uma consulta a mais, e so aqui: e a tela de um produto so. Carregar as
         * caracteristicas junto de cada item de uma listagem custaria uma ida ao banco por
         * produto, com a aplicacao a 5.000 km dele (D-45).
         */
        return ProdutoDetalhadoResponse.de(
                produto, produtoDao.buscarAtributosDe(produtoId));
    }
}
