package br.com.jence.backend;

import org.glassfish.grizzly.http.server.HttpServer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O servidor que a Main sobe, sem banco: so abrir, responder e fechar. */
class MainTest {

    static int portaLivre() throws IOException {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        }
    }

    @Test
    @DisplayName("startServer abre a porta pedida e o Jersey responde")
    void sobeNaPortaPedida() throws Exception {
        int porta = portaLivre();
        HttpServer server = Main.startServer(porta);
        try {
            assertTrue(server.isStarted());

            HttpResponse<String> resposta = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + porta + "/nao-existe")).build(),
                    HttpResponse.BodyHandlers.ofString());

            assertEquals(404, resposta.statusCode(), "quem responde e o Jersey, nao a conexao recusada");
        } finally {
            server.shutdownNow();
        }
        assertFalse(server.isStarted());
    }
}
