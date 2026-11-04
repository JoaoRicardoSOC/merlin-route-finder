package br.com.jence.backend;

import br.com.jence.backend.application.dto.MapaResponse;
import br.com.jence.backend.application.dto.PontoMapaResponse;
import br.com.jence.backend.application.usecase.ConsultarMapaUseCase;
import br.com.jence.backend.domain.entity.BlocoMapa;
import br.com.jence.backend.domain.entity.PlantaDaLoja;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A planta contra a massa real: o mapa so serve se o que ele desenha bater com onde os
 * produtos de fato estao.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class MapaIntegracaoTest {

    @Autowired ConsultarMapaUseCase consultarMapa;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired PontoMapaRepository pontoMapaRepository;

    private Optional<BlocoMapa> blocoDe(String rotulo) {
        return PlantaDaLoja.blocos().stream()
                .filter(bloco -> bloco.rotulo().equals(rotulo))
                .findFirst();
    }

    private List<Produto> catalogo() {
        Pagina<Produto> pagina = produtoRepository.buscarPaginado(0, 1000);
        assertTrue(pagina.totalElementos() <= pagina.conteudo().size(),
                "catalogo maior que a leitura do teste invalidaria a verificacao");
        return pagina.conteudo();
    }

    // ---------------------------------------------------------------- planta contra massa

    @Test
    @DisplayName("toda secao da massa tem bloco, e todo bloco tem secao")
    void plantaEMassaCobremAsMesmasSecoes() {
        List<String> naMassa = pontoMapaRepository.buscarPorTipo(TipoPonto.PRATELEIRA).stream()
                .map(PontoMapa::getCorredor)
                .sorted()
                .toList();
        List<String> naPlanta = PlantaDaLoja.blocos().stream()
                .map(BlocoMapa::rotulo)
                .sorted()
                .toList();

        /*
         * Secao sem bloco: os produtos dela aparecem soltos, sem corredor em volta. Bloco sem
         * secao: um corredor vazio desenhado na tela, que o cliente procura e nao acha.
         */
        assertEquals(naPlanta, naMassa);
    }

    @Test
    @DisplayName("nenhum produto cai fora do proprio corredor")
    void produtoDentroDoBloco() {
        for (Produto produto : catalogo()) {
            PontoMapa ponto = produto.getPontoMapa();
            BlocoMapa bloco = blocoDe(ponto.getCorredor()).orElseThrow(
                    () -> new AssertionError("sem bloco para a secao " + ponto.getCorredor()));

            assertTrue(bloco.contem(ponto.getCoordenadaX(), ponto.getCoordenadaY()),
                    "%s esta em (%d,%d), fora do bloco %s".formatted(
                            produto.getSku(), ponto.getCoordenadaX(), ponto.getCoordenadaY(),
                            bloco.rotulo()));
        }
    }

    @Test
    @DisplayName("nenhuma placa de QR fica dentro de um bloco")
    void placasEmCorredorDePassagem() {
        /*
         * A decisao do card dos QR Codes: os adesivos ficam em corredores de passagem e
         * cruzamentos, nao dentro das secoes - o cliente escaneia enquanto anda. Uma placa
         * desenhada em cima de um corredor contradiz isso na tela.
         */
        for (PontoMapa placa : pontoMapaRepository.buscarPorTipo(TipoPonto.QR_CODE)) {
            Optional<BlocoMapa> dentroDe = PlantaDaLoja.blocos().stream()
                    .filter(b -> b.contem(placa.getCoordenadaX(), placa.getCoordenadaY()))
                    .findFirst();

            assertTrue(dentroDe.isEmpty(),
                    "a placa %s (%d,%d) caiu dentro de um corredor".formatted(
                            placa.getCodigoCurto(), placa.getCoordenadaX(), placa.getCoordenadaY()));
        }
    }

    // ---------------------------------------------------------------- o endpoint

    @Test
    @DisplayName("o mapa devolve os blocos e os pontos que nao sao prateleira")
    void mapaCompleto() {
        MapaResponse mapa = consultarMapa.executar();

        System.out.printf(">>> mapa: %d bloco(s), %d ponto(s)%n",
                mapa.blocos().size(), mapa.pontos().size());

        assertEquals(100, mapa.largura());
        assertEquals(100, mapa.altura());
        assertEquals(PlantaDaLoja.blocos().size(), mapa.blocos().size());

        assertFalse(mapa.pontos().isEmpty());
        assertTrue(mapa.pontos().stream().noneMatch(ponto -> ponto.tipo() == TipoPonto.PRATELEIRA),
                "prateleira ja e representada pelo bloco; repeti-la como ponto duplicaria a secao");
        List<TipoPonto> tipos = mapa.pontos().stream().map(PontoMapaResponse::tipo).toList();
        assertTrue(tipos.contains(TipoPonto.CAIXA));
        assertTrue(tipos.contains(TipoPonto.BANHEIRO));
        assertTrue(tipos.contains(TipoPonto.QR_CODE));
    }

    @Test
    @DisplayName("o mapa e igual entre chamadas: nao depende de sessao nem de estado")
    void mapaEstavel() {
        // E o que permite ao frontend busca-lo uma vez e guardar no aparelho.
        assertEquals(consultarMapa.executar(), consultarMapa.executar());
    }
}
