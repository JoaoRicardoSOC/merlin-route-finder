package br.com.jence.backend.service;

import br.com.jence.backend.dto.ListaRoteiroResponse;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UC-005: consulta a lista de compras montada na sessao.
 * <p>
 * Nao renova o TTL da sessao: consultar e uma leitura e nao deveria gravar nada. A renovacao
 * acontece nas acoes que de fato indicam o cliente montando a lista (adicionar e remover).
 */
@Service
public class ConsultarListaRoteiroUseCase {

    private final ListaRoteiroDao listaRoteiroDao;

    public ConsultarListaRoteiroUseCase(ListaRoteiroDao listaRoteiroDao) {
        this.listaRoteiroDao = listaRoteiroDao;
    }

    public ListaRoteiroResponse executar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            ListaRoteiroResponse resposta = consultar(sessaoId);
            transacao.confirmar();
            return resposta;
        }
    }

    private ListaRoteiroResponse consultar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        return listaRoteiroDao.buscarPorSessao(sessaoId)
                .map(ListaRoteiroResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));
    }
}
