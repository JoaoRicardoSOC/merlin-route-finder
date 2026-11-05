package br.com.jence.backend.service;

import br.com.jence.backend.dto.SecaoResponse;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.factory.Transacao;

import java.sql.SQLException;
import java.util.List;

/** As secoes da loja, para o menu de navegacao do catalogo. */
public class ListarSecoesUseCase {

    private final ProdutoDao produtoDao;

    public ListarSecoesUseCase(ProdutoDao produtoDao) {
        this.produtoDao = produtoDao;
    }

    public List<SecaoResponse> executar() throws SQLException {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            List<SecaoResponse> secoes = produtoDao.listarSecoes().stream()
                    .map(SecaoResponse::de)
                    .toList();
            transacao.confirmar();
            return secoes;
        }
    }
}
