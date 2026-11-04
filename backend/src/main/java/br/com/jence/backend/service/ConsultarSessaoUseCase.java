package br.com.jence.backend.service;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Consulta o estado atual da sessao.
 * <p>
 * E por aqui que o celular descobre <b>onde o cliente esta</b> ao reabrir a pagina, alem de
 * saber se a sessao continua valida. Por isso carrega tambem a lista: a posicao pode vir do
 * ultimo item coletado, e nao apenas da placa lida.
 */
@Service
public class ConsultarSessaoUseCase {

    private final SessaoDao sessaoDao;
    private final ListaRoteiroDao listaRoteiroDao;

    public ConsultarSessaoUseCase(SessaoDao sessaoDao, ListaRoteiroDao listaRoteiroDao) {
        this.sessaoDao = sessaoDao;
        this.listaRoteiroDao = listaRoteiroDao;
    }

    public SessaoResponse executar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            SessaoResponse resposta = consultar(sessaoId);
            transacao.confirmar();
            return resposta;
        }
    }

    private SessaoResponse consultar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        Sessao sessao = sessaoDao.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        return SessaoResponse.de(sessao,
                listaRoteiroDao.buscarPorSessao(sessaoId).orElse(null));
    }
}
