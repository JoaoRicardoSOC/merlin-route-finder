package br.com.jence.backend.service;

import br.com.jence.backend.dto.BlocoMapaResponse;
import br.com.jence.backend.dto.MapaResponse;
import br.com.jence.backend.dto.PontoMapaResponse;
import br.com.jence.backend.model.PlantaDaLoja;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.PontoMapa;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Monta a planta da loja para a tela de mapa.
 * <p>
 * <b>Nao toca em sessao.</b> O mapa e o mesmo para todo cliente: o que varia entre um e outro -
 * onde ele esta e o que ele escolheu - vem das respostas de sessao e de lista. Manter o mapa
 * independente permite que o frontend o busque uma vez e o guarde, inclusive para funcionar
 * com a conexao caindo dentro da loja.
 */
@Service
public class ConsultarMapaUseCase {

    /** O grid em que tudo no mapa e expresso. Vai junto para o frontend nao precisar supor. */
    public static final int LADO_DO_GRID = 100;

    /** Tudo que nao e prateleira aparece no mapa como marcador proprio. */
    private static final List<TipoPonto> TIPOS_DE_MARCADOR =
            List.of(TipoPonto.CAIXA, TipoPonto.BANHEIRO, TipoPonto.QR_CODE);

    private final PontoMapaDao pontoMapaDao;

    public ConsultarMapaUseCase(PontoMapaDao pontoMapaDao) {
        this.pontoMapaDao = pontoMapaDao;
    }

    public MapaResponse executar() throws SQLException {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            MapaResponse mapa = montar();
            transacao.confirmar();
            return mapa;
        }
    }

    private MapaResponse montar() throws SQLException {
        List<PontoMapa> marcadores = new ArrayList<>();
        for (TipoPonto tipo : TIPOS_DE_MARCADOR) {
            marcadores.addAll(pontoMapaDao.buscarPorTipo(tipo));
        }

        List<PontoMapaResponse> pontos = marcadores.stream()
                .sorted(Comparator.comparing(p -> p.getTipo().name()))
                .map(PontoMapaResponse::de)
                .toList();

        MapaResponse mapa = new MapaResponse();
        mapa.setLargura(LADO_DO_GRID);
        mapa.setAltura(LADO_DO_GRID);
        mapa.setBlocos(PlantaDaLoja.blocos().stream().map(BlocoMapaResponse::de).toList());
        mapa.setPontos(pontos);
        return mapa;
    }
}
