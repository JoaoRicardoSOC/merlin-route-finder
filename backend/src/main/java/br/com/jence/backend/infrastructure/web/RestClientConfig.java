package br.com.jence.backend.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cliente HTTP usado nas chamadas de saida da aplicacao (hoje, a API de IA).
 * <p>
 * Construido explicitamente em vez de depender de autoconfiguracao: e aqui que os tempos
 * limite ficam visiveis. Numa demonstracao ao vivo, esperar por uma API externa travada e
 * pior do que degradar rapido - o caso de uso tem fallback, mas so se a chamada devolver.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient.Builder restClientBuilder(
            @Value("${merlin.http.connect-timeout-seconds:5}") long conexaoSegundos,
            @Value("${merlin.http.read-timeout-seconds:20}") long leituraSegundos) {

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(conexaoSegundos))
                .build();

        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(Duration.ofSeconds(leituraSegundos));

        return RestClient.builder().requestFactory(fabrica);
    }
}
