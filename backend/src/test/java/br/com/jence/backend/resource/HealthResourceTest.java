package br.com.jence.backend.resource;

import br.com.jence.backend.Main;
import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** O health check do Render, chamado na aplicacao de verdade, sem banco. */
class HealthResourceTest {

    private HttpServer server;
    private int porta;

    @BeforeEach
    void subir() throws Exception {
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        server = Main.startServer(porta);
    }

    @AfterEach
    void descer() {
        server.shutdownNow();
    }

    @Test
    @DisplayName("GET /health responde 200 sem precisar do banco")
    void respondeSemBanco() throws Exception {
        HttpResponse<String> resposta = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/health")).build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, resposta.statusCode());
        assertEquals("OK", resposta.body());
    }
}
