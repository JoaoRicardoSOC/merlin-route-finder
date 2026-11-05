package br.com.jence.backend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A validacao manual do recentrar, que substitui o @NotBlank. */
class RecentrarSessaoRequestTest {

    @Test
    @DisplayName("codigo ausente ou em branco e recusado")
    void codigoEmBranco() {
        for (String codigo : new String[] {null, "", "  "}) {
            RecentrarSessaoRequest requisicao = new RecentrarSessaoRequest();
            requisicao.setCodigoPonto(codigo);

            List<StandardError.ValidationError> erros = requisicao.validar();

            assertEquals(1, erros.size(), "codigo: [" + codigo + "]");
            assertEquals("codigoPonto", erros.get(0).getField());
            assertEquals("informe o codigo da placa", erros.get(0).getMessage());
        }
    }

    @Test
    @DisplayName("com codigo, nao ha erro")
    void comCodigo() {
        RecentrarSessaoRequest requisicao = new RecentrarSessaoRequest();
        requisicao.setCodigoPonto("CEN-03");

        assertTrue(requisicao.validar().isEmpty());
    }
}
