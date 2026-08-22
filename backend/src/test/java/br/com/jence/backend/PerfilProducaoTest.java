package br.com.jence.backend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ActiveProfiles;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/** Sobe a aplicacao com o perfil de producao ativo, num servidor HTTP real. */
@Tag("integracao")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("prod")
class PerfilProducaoTest {

    @LocalServerPort int porta;
    @Autowired Environment environment;

    @Value("${merlin.cors.allowed-origins}") String[] origens;

    @Test
    void aplicacaoSobeComPerfilDeProducao() throws Exception {
        assertThat(environment.getActiveProfiles()).contains("prod");

        assertThat(environment.getProperty("spring.jpa.show-sql"))
                .as("SQL nao pode ser logado em producao")
                .isEqualTo("false");

        System.out.println(">>> perfil ativo    : prod");
        System.out.println(">>> show-sql        : " + environment.getProperty("spring.jpa.show-sql"));
        System.out.println(">>> origens CORS    : " + String.join(", ", origens));

        // a API responde normalmente
        HttpResponse<String> res = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + porta + "/api/v1/produtos?size=1"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(res.statusCode()).isEqualTo(200);
        System.out.println(">>> GET /api/v1/produtos -> " + res.statusCode());

        // Swagger continua acessivel: e vitrine da API na banca (D-34)
        HttpResponse<String> docs = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder()
                        .uri(URI.create("http://localhost:" + porta + "/v3/api-docs"))
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(docs.statusCode()).isEqualTo(200);
        System.out.println(">>> GET /v3/api-docs     -> " + docs.statusCode());
    }
}
