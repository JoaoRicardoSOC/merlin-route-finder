package br.com.jence.backend.resource;

import br.com.jence.backend.dto.BlocoMapaResponse;
import br.com.jence.backend.dto.MapaResponse;
import br.com.jence.backend.dto.PontoMapaResponse;
import br.com.jence.backend.model.BlocoMapa;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.service.ConsultarMapaUseCase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** GET /mapa pelo Jersey, com o caso de uso falso. */
class MapaResourceTest {

    /** Devolve o mapa combinado, ou lanca a falha combinada. */
    static class ConsultarMapaFalso extends ConsultarMapaUseCase {
        MapaResponse mapa;
        SQLException falha;

        ConsultarMapaFalso() {
            super(null);
        }

        @Override
        public MapaResponse executar() throws SQLException {
            if (falha != null) {
                throw falha;
            }
            return mapa;
        }
    }

    private MapaResponse mapa() {
        MapaResponse mapa = new MapaResponse();
        mapa.setLargura(100);
        mapa.setAltura(100);
        mapa.setBlocos(List.of(BlocoMapaResponse.de(new BlocoMapa("Tintas", 10, 20, 30, 8))));
        mapa.setPontos(List.of(PontoMapaResponse.de(
                new PontoMapa(UUID.randomUUID(), TipoPonto.CAIXA, "Caixas", 50, 95))));
        return mapa;
    }

    @Test
    @DisplayName("GET /mapa devolve a planta no formato do contrato")
    void consultar() throws Exception {
        ConsultarMapaFalso consultar = new ConsultarMapaFalso();
        consultar.mapa = mapa();

        try (ServidorDeTeste servidor = ServidorDeTeste.com(new MapaResource(consultar))) {
            ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/mapa");

            assertEquals(200, resposta.status());
            assertEquals(100, resposta.inteiro("largura"));
            assertEquals("Tintas", resposta.texto("blocos[0].rotulo"));
            assertEquals(30, resposta.inteiro("blocos[0].largura"));
            assertEquals("CAIXA", resposta.texto("pontos[0].tipo"));
            assertEquals(95, resposta.inteiro("pontos[0].coordenadaY"));
        }
    }

    @Test
    @DisplayName("banco fora do ar vira 500 com mensagem generica, sem vazar o detalhe")
    void falhaDoBanco() throws Exception {
        ConsultarMapaFalso consultar = new ConsultarMapaFalso();
        consultar.falha = new SQLException("ORA-12541: TNS:no listener");

        try (ServidorDeTeste servidor = ServidorDeTeste.com(new MapaResource(consultar))) {
            ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/mapa");

            assertEquals(500, resposta.status());
            assertEquals("Erro Interno do Servidor", resposta.texto("error"));
            assertEquals("/api/v1/mapa", resposta.texto("path"));
            assertFalse(resposta.corpo().contains("ORA-"));
        }
    }
}
