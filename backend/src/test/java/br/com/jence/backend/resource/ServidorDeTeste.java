package br.com.jence.backend.resource;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sobe o Grizzly com os Resources dados - montados com services falsos, sem banco - numa porta
 * livre, e faz as chamadas com o {@code HttpClient} do JDK.
 * <p>
 * Os testes web usam este servidor no lugar do {@code MockMvc} do Spring: a requisicao passa
 * pelo Jersey de verdade, com a mesma leitura e escrita de JSON da aplicacao no ar.
 */
public final class ServidorDeTeste implements AutoCloseable {

    private static final Jsonb JSONB = JsonbBuilder.create();
    private static final Pattern INDICE = Pattern.compile("\\[(\\d+)]");

    private final HttpServer server;
    private final String base;
    private final HttpClient cliente = HttpClient.newHttpClient();

    private ServidorDeTeste(Object... resources) throws IOException {
        int porta;
        try (ServerSocket socket = new ServerSocket(0)) {
            porta = socket.getLocalPort();
        }
        ResourceConfig rc = new ResourceConfig();
        for (Object resource : resources) {
            rc.register(resource);
        }
        base = "http://localhost:" + porta;
        server = GrizzlyHttpServerFactory.createHttpServer(URI.create(base + "/"), rc);
    }

    /** Sobe o servidor com estas instancias de Resource. */
    public static ServidorDeTeste com(Object... resources) throws IOException {
        return new ServidorDeTeste(resources);
    }

    public Resposta get(String caminho) throws Exception {
        return enviar("GET", caminho, null, null);
    }

    public Resposta delete(String caminho) throws Exception {
        return enviar("DELETE", caminho, null, null);
    }

    /** POST, PUT ou PATCH sem corpo. */
    public Resposta semCorpo(String metodo, String caminho) throws Exception {
        return enviar(metodo, caminho, null, null);
    }

    /** POST, PUT ou PATCH com corpo JSON. */
    public Resposta json(String metodo, String caminho, String corpo) throws Exception {
        return enviar(metodo, caminho, "application/json", corpo);
    }

    public Resposta enviar(String metodo, String caminho, String tipo, String corpo) throws Exception {
        HttpRequest.Builder requisicao = HttpRequest.newBuilder(URI.create(base + caminho));
        if (tipo != null) {
            requisicao.header("Content-Type", tipo);
        }
        requisicao.method(metodo, corpo == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(corpo));
        HttpResponse<String> resposta =
                cliente.send(requisicao.build(), HttpResponse.BodyHandlers.ofString());
        return new Resposta(resposta.statusCode(), resposta.body(),
                resposta.headers().firstValue("Location"));
    }

    @Override
    public void close() {
        server.shutdownNow();
    }

    /** O que voltou da chamada, com leitura de campos do JSON por caminho ({@code a.b[0].c}). */
    public record Resposta(int status, String corpo, Optional<String> location) {

        /** O valor no caminho, ou {@code null} se algum trecho do caminho nao existir. */
        public Object json(String caminho) {
            Object atual = JSONB.fromJson(corpo, Object.class);
            for (String trecho : caminho.split("\\.")) {
                String nome = trecho.replaceAll("\\[\\d+]", "");
                if (!nome.isEmpty()) {
                    if (!(atual instanceof Map<?, ?> mapa)) {
                        return null;
                    }
                    atual = mapa.get(nome);
                }
                Matcher indice = INDICE.matcher(trecho);
                while (indice.find()) {
                    if (!(atual instanceof List<?> lista)) {
                        return null;
                    }
                    atual = lista.get(Integer.parseInt(indice.group(1)));
                }
            }
            return atual;
        }

        public String texto(String caminho) {
            Object valor = json(caminho);
            return valor == null ? null : valor.toString();
        }

        public int inteiro(String caminho) {
            return ((BigDecimal) json(caminho)).intValueExact();
        }

        public int tamanho(String caminho) {
            return ((List<?>) json(caminho)).size();
        }
    }
}
