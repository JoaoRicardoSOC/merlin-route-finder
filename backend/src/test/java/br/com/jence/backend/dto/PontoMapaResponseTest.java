package br.com.jence.backend.dto;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PontoMapaResponseTest {

    @Test
    @DisplayName("copia cada campo do ponto, sem trocar as coordenadas")
    void copiaOsCampos() {
        UUID id = UUID.randomUUID();
        PontoMapa ponto = new PontoMapa(id, TipoPonto.QR_CODE, "Tintas", 32, 10, "TIN-01");

        PontoMapaResponse resposta = PontoMapaResponse.de(ponto);

        assertEquals(id, resposta.getId());
        assertEquals(TipoPonto.QR_CODE, resposta.getTipo());
        assertEquals("Tintas", resposta.getCorredor());
        assertEquals(ponto.getCodigoCurto(), resposta.getCodigoCurto());
        assertEquals(32, resposta.getCoordenadaX());
        assertEquals(10, resposta.getCoordenadaY());
    }

    @Test
    @DisplayName("ponto ausente vira nulo")
    void pontoAusente() {
        assertNull(PontoMapaResponse.de(null));
    }
}
