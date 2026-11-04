package br.com.jence.backend.service;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UC-014 (parte 2): encerra a jornada do cliente.
 * <p>
 * Nao exige que todos os itens estejam coletados: o cliente pode desistir de um produto e ir
 * ao caixa. Tambem nao e disparado automaticamente ao coletar o ultimo item - depois de pegar
 * tudo ele ainda precisa chegar ao caixa, e encerrar ali mataria a navegacao no trecho final.
 */
@Service
public class ConcluirRotaUseCase {

    private final SessaoDao sessaoDao;

    public ConcluirRotaUseCase(SessaoDao sessaoDao) {
        this.sessaoDao = sessaoDao;
    }

    public SessaoResponse executar(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        try (Transacao transacao = Transacao.iniciar()) {
            SessaoResponse resposta = concluir(sessaoId);
            transacao.confirmar();
            return resposta;
        }
    }

    private SessaoResponse concluir(UUID sessaoId) throws SQLException, RecursoNaoEncontradoException {
        Sessao sessao = sessaoDao.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        sessao.encerrar();
        return SessaoResponse.de(sessaoDao.salvar(sessao));
    }
}
