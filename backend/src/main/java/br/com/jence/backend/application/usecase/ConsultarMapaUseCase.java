package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.BlocoMapaResponse;
import br.com.jence.backend.application.dto.MapaResponse;
import br.com.jence.backend.application.dto.PontoMapaResponse;
import br.com.jence.backend.domain.entity.PlantaDaLoja;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@RequiredArgsConstructor
public class ConsultarMapaUseCase {

    /** O grid em que tudo no mapa e expresso. Vai junto para o frontend nao precisar supor. */
    public static final int LADO_DO_GRID = 100;

    /** Tudo que nao e prateleira aparece no mapa como marcador proprio. */
    private static final List<TipoPonto> TIPOS_DE_MARCADOR =
            List.of(TipoPonto.CAIXA, TipoPonto.BANHEIRO, TipoPonto.QR_CODE);

    private final PontoMapaRepository pontoMapaRepository;

    @Transactional(readOnly = true)
    public MapaResponse executar() {
        List<PontoMapaResponse> pontos = TIPOS_DE_MARCADOR.stream()
                .flatMap(tipo -> pontoMapaRepository.buscarPorTipo(tipo).stream())
                .sorted(Comparator.comparing(p -> p.getTipo().name()))
                .map(PontoMapaResponse::de)
                .toList();

        return new MapaResponse(
                LADO_DO_GRID,
                LADO_DO_GRID,
                PlantaDaLoja.blocos().stream().map(BlocoMapaResponse::de).toList(),
                pontos);
    }
}
