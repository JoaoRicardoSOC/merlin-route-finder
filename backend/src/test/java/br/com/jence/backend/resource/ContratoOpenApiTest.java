package br.com.jence.backend.resource;

import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O contrato escrito a mao contra o que a aplicacao de fato expoe.
 * <p>
 * Existem duas fontes de verdade sobre a API: o {@code openapi.yaml} versionado, redigido no
 * primeiro card para a dupla de frontend integrar sem esperar a implementacao, e as anotacoes
 * dos Resources. <b>Nada garante que as duas concordem</b> - e quem descobriria a divergencia
 * seria o frontend, integrando.
 * <p>
 * Compara rota e metodo, que e o nivel em que a divergencia quebra a integracao: um endpoint
 * que existe no papel e nao no codigo, ou o contrario, ou um verbo trocado.
 * <p>
 * Sem banco e sem servidor: as rotas do codigo sao lidas por reflexao das anotacoes JAX-RS, e
 * as do contrato, lendo o {@code openapi.yaml} linha a linha.
 */
class ContratoOpenApiTest {

    private static final String PACOTE = "br.com.jence.backend.resource";

    /** O contrato omite este prefixo, que vive no servers[]. */
    private static final String PREFIXO = "/api/v1";

    /** Um caminho sob paths: (dois espacos) e um verbo sob ele (quatro espacos). */
    private static final Pattern ROTA = Pattern.compile("^  (/\\S*):\\s*$");
    private static final Pattern VERBO = Pattern.compile("^    (get|post|put|patch|delete):\\s*$");

    private Set<String> operacoesDeclaradas() throws Exception {
        Set<String> operacoes = new TreeSet<>();
        try (InputStream entrada = getClass().getResourceAsStream("/openapi/openapi.yaml");
             BufferedReader leitor = new BufferedReader(
                     new InputStreamReader(entrada, StandardCharsets.UTF_8))) {
            boolean emPaths = false;
            String rota = null;
            String linha;
            while ((linha = leitor.readLine()) != null) {
                if (!linha.isEmpty() && !Character.isWhitespace(linha.charAt(0))) {
                    // Chave de primeiro nivel: so o bloco paths interessa.
                    emPaths = linha.startsWith("paths:");
                    continue;
                }
                if (!emPaths) {
                    continue;
                }
                Matcher novaRota = ROTA.matcher(linha);
                if (novaRota.matches()) {
                    rota = novaRota.group(1);
                    continue;
                }
                Matcher verbo = VERBO.matcher(linha);
                if (verbo.matches() && rota != null) {
                    operacoes.add(verbo.group(1).toUpperCase() + " " + PREFIXO + rota);
                }
            }
        }
        return operacoes;
    }

    private Set<String> operacoesImplementadas() throws Exception {
        Set<String> operacoes = new TreeSet<>();
        for (Class<?> resource : classesDoPacote()) {
            Path base = resource.getAnnotation(Path.class);
            if (base == null) {
                continue;
            }
            for (Method metodo : resource.getDeclaredMethods()) {
                String verbo = verboHttp(metodo);
                if (verbo == null) {
                    continue;
                }
                Path sub = metodo.getAnnotation(Path.class);
                String rota = juntar(base.value(), sub == null ? "" : sub.value());
                if (rota.startsWith(PREFIXO)) {
                    operacoes.add(verbo + " " + rota);
                }
            }
        }
        return operacoes;
    }

    /** O verbo de um metodo JAX-RS: a anotacao marcada com @HttpMethod (@GET, @POST...). */
    private static String verboHttp(Method metodo) {
        for (Annotation anotacao : metodo.getAnnotations()) {
            HttpMethod verbo = anotacao.annotationType().getAnnotation(HttpMethod.class);
            if (verbo != null) {
                return verbo.value();
            }
        }
        return null;
    }

    private static String juntar(String base, String sub) {
        String rota = ("/" + base + "/" + sub).replaceAll("/+", "/");
        return rota.length() > 1 && rota.endsWith("/") ? rota.substring(0, rota.length() - 1) : rota;
    }

    /** As classes compiladas do pacote, para que um Resource novo entre sem ser listado aqui. */
    private static Set<Class<?>> classesDoPacote() throws Exception {
        URL pasta = ContratoOpenApiTest.class.getClassLoader().getResource(PACOTE.replace('.', '/'));
        Set<Class<?>> classes = new LinkedHashSet<>();
        // O pacote existe nas classes de producao e nas de teste; varre os dois.
        for (URL url : Collections.list(
                ContratoOpenApiTest.class.getClassLoader().getResources(PACOTE.replace('.', '/')))) {
            File[] arquivos = new File(url.toURI()).listFiles((dir, nome) ->
                    nome.endsWith(".class") && !nome.contains("$"));
            if (arquivos == null) {
                continue;
            }
            for (File arquivo : arquivos) {
                String nome = arquivo.getName().replace(".class", "");
                classes.add(Class.forName(PACOTE + "." + nome));
            }
        }
        assertTrue(pasta != null && !classes.isEmpty(), "nenhuma classe encontrada em " + PACOTE);
        return classes;
    }

    @Test
    @DisplayName("o contrato escrito a mao descreve exatamente o que a API expoe")
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

        assertFalse(declaradas.isEmpty(), "a leitura do openapi.yaml nao encontrou nenhuma rota");
        assertTrue(soNoPapel.isEmpty(),
                "o contrato promete endpoints que a aplicacao nao tem - o frontend "
                        + "integraria contra algo inexistente: " + soNoPapel);
        assertTrue(soNoCodigo.isEmpty(),
                "a aplicacao expoe endpoints que o contrato nao descreve - o frontend "
                        + "nem saberia que existem: " + soNoCodigo);
    }
}
