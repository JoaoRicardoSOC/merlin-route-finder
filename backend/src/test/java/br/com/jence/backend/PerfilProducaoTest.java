package br.com.jence.backend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Sobe a aplicacao com o perfil de producao ativo, num servidor HTTP real. */
@Tag("integracao")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
class PerfilProducaoTest {

    @LocalServerPort int porta;
    @Autowired Environment environment;

    @Test
    void aplicacaoSobeComPerfilDeProducao() throws Exception {
        assertTrue(Arrays.asList(environment.getActiveProfiles()).contains("prod"));

        System.out.println(">>> perfil ativo    : prod");

        // a API responde normalmente
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + porta + "/api/v1/produtos?size=1"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, res.statusCode());
        System.out.println(">>> GET /api/v1/produtos -> " + res.statusCode());

        // Swagger continua acessivel: e vitrine da API na banca (D-34)
        HttpResponse<String> docs = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + porta + "/v3/api-docs"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertEquals(200, docs.statusCode());
        System.out.println(">>> GET /v3/api-docs     -> " + docs.statusCode());
    }
}
