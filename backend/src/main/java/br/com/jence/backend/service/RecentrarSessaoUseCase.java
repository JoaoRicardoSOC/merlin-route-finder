package br.com.jence.backend.service;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O cliente se perdeu e leu outra placa da loja.
 * <p>
 * Nao cria sessao nova: a lista que ele montou continua inteira, com o que ja foi coletado
 * preservado. Muda apenas onde ele esta.
 */
@Service
public class RecentrarSessaoUseCase {

    private final SessaoDao sessaoDao;
    private final ListaRoteiroDao listaRoteiroDao;
    private final PontoMapaDao pontoMapaDao;

    public RecentrarSessaoUseCase(SessaoDao sessaoDao,
                                  ListaRoteiroDao listaRoteiroDao,
                                  PontoMapaDao pontoMapaDao) {
        this.sessaoDao = sessaoDao;
        this.listaRoteiroDao = listaRoteiroDao;
        this.pontoMapaDao = pontoMapaDao;
    }

    public SessaoResponse executar(UUID sessaoId, String codigoPonto)
            throws SQLException, RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        try (Transacao transacao = Transacao.iniciar()) {
            SessaoResponse resposta = recentrar(sessaoId, codigoPonto);
            transacao.confirmar();
            return resposta;
        }
    }

    private SessaoResponse recentrar(UUID sessaoId, String codigoPonto)
            throws SQLException, RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        Sessao sessao = sessaoDao.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        /*
         * Aqui codigo desconhecido e erro, ao contrario da criacao da sessao, que aceita e
         * nasce sem posicao. A diferenca e o custo de recusar: na entrada, recusar barraria o
         * cliente do sistema inteiro por causa de um adesivo; aqui ele ja tem sessao
         * funcionando, e avisar que a placa nao foi encontrada e acionavel - ele tenta de
         * novo. Aceitar em silencio seria pior: ele acharia que funcionou. Ver D-57.
         */
        PontoMapa ponto = pontoMapaDao.buscarPorCodigoCurto(codigoPonto)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Placa de localizacao", codigoPonto));

        sessao.recentrarEm(ponto, LocalDateTime.now());

        // Quem para para ler uma placa esta ativo na loja, e nao pode perder a sessao por
        // inatividade logo depois de pedir ajuda para se achar.
        sessao.renovarSessao();
        sessaoDao.salvar(sessao);

        return SessaoResponse.de(sessao,
                listaRoteiroDao.buscarPorSessao(sessaoId).orElse(null));
    }
}
