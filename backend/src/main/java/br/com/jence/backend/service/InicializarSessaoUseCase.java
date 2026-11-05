package br.com.jence.backend.service;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;

import java.sql.SQLException;
import java.util.UUID;

/**
 * UC-001: inicializa a sessao quando o cliente entra pela placa de localizacao.
 * <p>
 * O codigo da placa chega das duas formas previstas no fluxo: embutido na URL do QR Code, ou
 * digitado pelo cliente quando escanear nao deu certo. Para o caso de uso e o mesmo caminho.
 */
public class InicializarSessaoUseCase {

    private final SessaoDao sessaoDao;
    private final ListaRoteiroDao listaRoteiroDao;
    private final PontoMapaDao pontoMapaDao;

    public InicializarSessaoUseCase(SessaoDao sessaoDao,
                                    ListaRoteiroDao listaRoteiroDao,
                                    PontoMapaDao pontoMapaDao) {
        this.sessaoDao = sessaoDao;
        this.listaRoteiroDao = listaRoteiroDao;
        this.pontoMapaDao = pontoMapaDao;
    }

    /*
     * Transacional porque a sessao e a lista sao gravadas em duas chamadas: se a segunda
     * falhasse, restaria uma sessao sem lista e o proximo passo da jornada (adicionar
     * produto ao roteiro) quebraria com um erro dificil de diagnosticar.
     */
    public SessaoResponse executar(String codigoPonto) throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            SessaoResponse resposta = inicializar(codigoPonto);
            transacao.confirmar();
            return resposta;
        }
    }

    private SessaoResponse inicializar(String codigoPonto) throws SQLException {
        /*
         * Codigo invalido nao recusa a sessao: placa velha, loja remanejada ou erro de
         * digitacao deixam o cliente sem "voce esta aqui", mas com o sistema inteiro
         * funcionando. Recusar aqui seria barrar a entrada por causa de um adesivo.
         */
        PontoMapa ponto = pontoMapaDao.buscarPorCodigoCurto(codigoPonto).orElse(null);

        if (ponto == null && codigoPonto != null && !codigoPonto.isBlank()) {
            System.out.println(String.format("Sessao iniciada com codigo de placa desconhecido: %s. "
                    + "Nasce sem posicao.", codigoPonto));
        }

        Sessao sessao = sessaoDao.salvar(Sessao.iniciar(UUID.randomUUID(), ponto));

        listaRoteiroDao.salvar(ListaRoteiro.criarPara(UUID.randomUUID(), sessao.getId()));

        return SessaoResponse.de(sessao);
    }
}
