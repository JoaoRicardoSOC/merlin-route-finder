package br.com.jence.backend;

import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dto.MapaResponse;
import br.com.jence.backend.dto.PontoMapaResponse;
import br.com.jence.backend.service.ConsultarMapaUseCase;
import br.com.jence.backend.model.BlocoMapa;
import br.com.jence.backend.model.PlantaDaLoja;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.model.Pagina;
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
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();
    private final PontoMapaDao pontoMapaDao = BancoDeTeste.pontos();

    private Optional<BlocoMapa> blocoDe(String rotulo) {
        return PlantaDaLoja.blocos().stream()
                .filter(bloco -> bloco.rotulo().equals(rotulo))
                .findFirst();
    }

    private List<Produto> catalogo() throws Exception {
        Pagina<Produto> pagina = produtoDao.buscarPaginado(0, 1000);
        assertTrue(pagina.totalElementos() <= pagina.conteudo().size(),
                "catalogo maior que a leitura do teste invalidaria a verificacao");
        return pagina.conteudo();
    }

    // ---------------------------------------------------------------- planta contra massa

    @Test
    @DisplayName("toda secao da massa tem bloco, e todo bloco tem secao")
    void plantaEMassaCobremAsMesmasSecoes() throws Exception {
        List<String> naMassa = pontoMapaDao.buscarPorTipo(TipoPonto.PRATELEIRA).stream()
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
    void produtoDentroDoBloco() throws Exception {
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
    void placasEmCorredorDePassagem() throws Exception {
        /*
         * A decisao do card dos QR Codes: os adesivos ficam em corredores de passagem e
         * cruzamentos, nao dentro das secoes - o cliente escaneia enquanto anda. Uma placa
         * desenhada em cima de um corredor contradiz isso na tela.
         */
        for (PontoMapa placa : pontoMapaDao.buscarPorTipo(TipoPonto.QR_CODE)) {
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
    void mapaCompleto() throws Exception {
        MapaResponse mapa = consultarMapa.executar();

        System.out.printf(">>> mapa: %d bloco(s), %d ponto(s)%n",
                mapa.getBlocos().size(), mapa.getPontos().size());

        assertEquals(100, mapa.getLargura());
        assertEquals(100, mapa.getAltura());
        assertEquals(PlantaDaLoja.blocos().size(), mapa.getBlocos().size());

        assertFalse(mapa.getPontos().isEmpty());
        assertTrue(mapa.getPontos().stream().noneMatch(ponto -> ponto.getTipo() == TipoPonto.PRATELEIRA),
                "prateleira ja e representada pelo bloco; repeti-la como ponto duplicaria a secao");
        List<TipoPonto> tipos = mapa.getPontos().stream().map(PontoMapaResponse::getTipo).toList();
        assertTrue(tipos.contains(TipoPonto.CAIXA));
        assertTrue(tipos.contains(TipoPonto.BANHEIRO));
        assertTrue(tipos.contains(TipoPonto.QR_CODE));
    }

    @Test
    @DisplayName("o mapa e igual entre chamadas: nao depende de sessao nem de estado")
    void mapaEstavel() throws Exception {
        // E o que permite ao frontend busca-lo uma vez e guardar no aparelho.
        assertEquals(consultarMapa.executar(), consultarMapa.executar());
    }
}
