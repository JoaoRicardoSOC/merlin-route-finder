package br.com.jence.backend;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * O contrato escrito a mao contra o que a aplicacao de fato expoe.
 * <p>
 * Existem duas fontes de verdade sobre a API: o {@code openapi.yaml} versionado, redigido no
 * primeiro card para a dupla de frontend integrar sem esperar a implementacao, e o documento
 * que o springdoc gera a partir das anotacoes reais (D-30). <b>Nada garantia que as duas
 * concordassem</b> - e quem descobriria a divergencia seria o frontend, integrando.
 * <p>
 * Compara rota e metodo, que e o nivel em que a divergencia quebra a integracao: um endpoint
 * que existe no papel e nao no codigo, ou o contrario, ou um verbo trocado.
 */
@Tag("integracao")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ContratoOpenApiIntegracaoTest {

    @LocalServerPort int porta;
    @Autowired RestClient.Builder builder;

    private static final String PREFIXO = "/api/v1";

    /** Rotas que existem por serem infraestrutura, nao contrato de negocio. */
    private static final Set<String> FORA_DO_CONTRATO = Set.of();

    @SuppressWarnings("unchecked")
    private Set<String> operacoesDeclaradas() throws Exception {
        try (InputStream entrada = getClass().getResourceAsStream("/openapi/openapi.yaml")) {
            Map<String, Object> doc = new Yaml().load(entrada);
            Map<String, Map<String, Object>> paths =
                    (Map<String, Map<String, Object>>) doc.get("paths");

            // O contrato escrito a mao omite o prefixo /api/v1, que vive no servers[].
            return paths.entrySet().stream()
                    .flatMap(rota -> rota.getValue().keySet().stream()
                            .filter(ContratoOpenApiIntegracaoTest::ehVerboHttp)
                            .map(verbo -> verbo.toUpperCase() + " " + PREFIXO + rota.getKey()))
                    .collect(Collectors.toCollection(TreeSet::new));
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> operacoesImplementadas() {
        Map<String, Object> doc = builder.build().get()
                .uri("http://localhost:" + porta + "/v3/api-docs")
                .retrieve()
                .body(Map.class);

        Map<String, Map<String, Object>> paths =
                (Map<String, Map<String, Object>>) doc.get("paths");

        return paths.entrySet().stream()
                .filter(rota -> rota.getKey().startsWith(PREFIXO))
                .filter(rota -> !FORA_DO_CONTRATO.contains(rota.getKey()))
                .flatMap(rota -> rota.getValue().keySet().stream()
                        .filter(ContratoOpenApiIntegracaoTest::ehVerboHttp)
                        .map(verbo -> verbo.toUpperCase() + " " + rota.getKey()))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static boolean ehVerboHttp(String chave) {
        return List.of("get", "post", "put", "patch", "delete").contains(chave.toLowerCase());
    }

    @Test
    @DisplayName("SONDA: o contrato escrito a mao descreve exatamente o que a API expoe")
    void contratoEImplementacaoConcordam() throws Exception {
        Set<String> declaradas = operacoesDeclaradas();
        Set<String> implementadas = operacoesImplementadas();

        Set<String> soNoPapel = new TreeSet<>(declaradas);
        soNoPapel.removeAll(implementadas);

        Set<String> soNoCodigo = new TreeSet<>(implementadas);
        soNoCodigo.removeAll(declaradas);

        System.out.println(">>> declaradas no openapi.yaml: " + declaradas.size());
        System.out.println(">>> expostas pela aplicacao:    " + implementadas.size());
        soNoPapel.forEach(o -> System.out.println("    SO NO CONTRATO: " + o));
        soNoCodigo.forEach(o -> System.out.println("    SO NO CODIGO:   " + o));

        assertThat(soNoPapel)
                .as("o contrato promete endpoints que a aplicacao nao tem - o frontend "
                        + "integraria contra algo inexistente")
                .isEmpty();

        assertThat(soNoCodigo)
                .as("a aplicacao expoe endpoints que o contrato nao descreve - o frontend "
                        + "nem saberia que existem")
                .isEmpty();
    }
}
