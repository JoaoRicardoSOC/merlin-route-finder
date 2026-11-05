package br.com.jence.backend;

import br.com.jence.backend.resource.ServidorDeTeste;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * A aplicacao inteira sobe como no Render: carga de dados no banco e o servidor da Main com os
 * Resources e DAOs de verdade. Substitui o teste que so conferia se o contexto do Spring subia.
 */
@Tag("integracao")
class AplicacaoIntegracaoTest {

    @Test
    @DisplayName("sobe com a massa carregada, passa no health check e a API responde")
    void aplicacaoSobe() throws Exception {
        BancoDeTeste.carregarMassa();

        try (ServidorDeTeste servidor = ServidorDeTeste.daAplicacao()) {
            assertEquals(200, servidor.get("/health").status());

            ServidorDeTeste.Resposta produtos = servidor.get("/api/v1/produtos?size=1");
            System.out.println(">>> GET /api/v1/produtos -> " + produtos.status());

            assertEquals(200, produtos.status());
            assertEquals(1, produtos.tamanho("content"));
        }
    }
}
