package br.com.jence.backend.ia;

import br.com.jence.backend.config.Configuracao;
import br.com.jence.backend.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.model.FerramentaIA;
import br.com.jence.backend.model.MensagemIA;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testa o cliente sem rede externa, com respostas simuladas no formato real da API do Gemini
 * (confirmado por chamadas de verdade durante a implementacao).
 * <p>
 * O "Gemini" aqui e um servidor HTTP local, do proprio JDK ({@code com.sun.net.httpserver}), que
 * responde a partir de uma fila e guarda cada pedido recebido para o teste conferir.
 */
class GeminiClientTest {

    private static final String CAMINHO = "/v1beta/models/modelo-teste:generateContent";

    /** Uma resposta que o servidor falso devolve. */
    private record Resposta(int status, String corpo) {
    }

    /** Um pedido que o servidor falso recebeu. */
    private record Pedido(String caminho, String consulta, String corpo) {
    }

    private final Deque<Resposta> respostas = new ArrayDeque<>();
    private final List<Pedido> pedidos = Collections.synchronizedList(new ArrayList<>());

    private HttpServer servidor;
    private GeminiClient cliente;

    @BeforeEach
    void preparar() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", troca -> {
            String corpo = new String(troca.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            pedidos.add(new Pedido(troca.getRequestURI().getPath(), troca.getRequestURI().getQuery(), corpo));

            Resposta resposta;
            synchronized (respostas) {
                resposta = respostas.isEmpty() ? new Resposta(500, "sem resposta preparada") : respostas.poll();
            }
            byte[] bytes = resposta.corpo().getBytes(StandardCharsets.UTF_8);
            troca.getResponseHeaders().add("Content-Type", "application/json");
            troca.sendResponseHeaders(resposta.status(), bytes.length);
            try (OutputStream saida = troca.getResponseBody()) {
                saida.write(bytes);
            }
        });
        servidor.start();
        cliente = clienteCom("chave-teste");
    }

    @AfterEach
    void parar() {
        servidor.stop(0);
    }

    private GeminiClient clienteCom(String chave) {
        return new GeminiClient(new Configuracao(Map.of(
                "MERLIN_IA_BASE_URL", "http://localhost:" + servidor.getAddress().getPort(),
                "GEMINI_MODEL", "modelo-teste",
                "GEMINI_API_KEY", chave)));
    }

    private void responder(int status, String corpo) {
        respostas.add(new Resposta(status, corpo));
    }

    /** Confere que todos os pedidos foram para o endereco certo, com a chave, e quantos foram. */
    private void conferirPedidos(int quantidade) {
        assertEquals(quantidade, pedidos.size());
        for (Pedido pedido : pedidos) {
            assertEquals(CAMINHO, pedido.caminho());
            assertEquals("key=chave-teste", pedido.consulta());
        }
    }

    @Test
    @DisplayName("extrai o texto de uma resposta simples")
    void respostaDeTexto() throws AssistenteIAIndisponivelException {
        responder(200, """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"A lixa grao 150 serve como substituto."}]},"finishReason":"STOP"}]}
                """);

        String resposta = cliente.conversar("instrucao", List.of(MensagemIA.doCliente("e ai?")));

        assertEquals("A lixa grao 150 serve como substituto.", resposta);
        conferirPedidos(1);
    }

    @Test
    @DisplayName("executa a ferramenta pedida e devolve o resultado ao modelo")
    void cicloDeFerramenta() throws AssistenteIAIndisponivelException {
        // 1a resposta: o modelo pede a ferramenta
        responder(200, """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"buscar_substitutos","args":{"corredor":"Tintas"},"id":"call_1"},
                   "thoughtSignature":"assinatura-do-provedor"}]},"finishReason":"STOP"}]}
                """);

        // 2a resposta: com o resultado em maos, ele conclui
        responder(200, """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"Leve a Lixa d'Agua Grao 150, no mesmo corredor."}]},"finishReason":"STOP"}]}
                """);

        AtomicReference<String> ferramentaChamada = new AtomicReference<>();
        AtomicReference<Map<String, Object>> argumentosRecebidos = new AtomicReference<>();

        String resposta = cliente.conversar(
                "instrucao",
                List.of(MensagemIA.doCliente("a lixa acabou")),
                List.of(new FerramentaIA("buscar_substitutos", "busca substitutos",
                        List.of(FerramentaIA.ParametroIA.obrigatorio("corredor", "onde o cliente esta")))),
                (nome, argumentos) -> {
                    ferramentaChamada.set(nome);
                    argumentosRecebidos.set(argumentos);
                    return Map.of("produtos", List.of(Map.of("nome", "Lixa d'Agua Grao 150")));
                });

        assertEquals("buscar_substitutos", ferramentaChamada.get());
        assertEquals("Tintas", argumentosRecebidos.get().get("corredor"));
        assertTrue(resposta.contains("Lixa d'Agua Grao 150"));
        conferirPedidos(2);

        // O segundo pedido devolve o resultado da ferramenta e o turno do modelo inteiro.
        String segundo = pedidos.get(1).corpo();
        assertTrue(segundo.contains("functionResponse"));
        assertTrue(segundo.contains("call_1"));
        assertTrue(segundo.contains("thoughtSignature"));
    }

    @Test
    @DisplayName("cota estourada e tentada de novo antes de desistir")
    void erroDoProvedor() {
        // O tier gratuito estoura cota com facilidade, e a falha costuma passar em segundos.
        responder(429, "{}");
        responder(429, "{}");
        responder(429, "{}");

        AssistenteIAIndisponivelException e = assertThrows(AssistenteIAIndisponivelException.class,
                () -> cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))));
        assertTrue(e.getMessage().contains("indisponivel"));

        conferirPedidos(3);
    }

    @Test
    @DisplayName("falha transitoria seguida de sucesso devolve a resposta")
    void recuperaDeFalhaTransitoria() throws AssistenteIAIndisponivelException {
        responder(500, "{}");
        responder(200, """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"Resposta apos o provedor se recuperar."}]},"finishReason":"STOP"}]}
                """);

        String resposta = cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi")));

        assertEquals("Resposta apos o provedor se recuperar.", resposta);
        conferirPedidos(2);
    }

    @Test
    @DisplayName("resposta sem candidatos (filtro de conteudo) vira indisponibilidade")
    void respostaBloqueada() {
        responder(200, """
                {"promptFeedback":{"blockReason":"SAFETY"}}
                """);

        AssistenteIAIndisponivelException e = assertThrows(AssistenteIAIndisponivelException.class,
                () -> cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))));
        assertTrue(e.getMessage().contains("nao produziu resposta"));
    }

    @Test
    @DisplayName("erro do cliente (4xx que nao e cota) nao e tentado de novo")
    void erroDoClienteNaoInsiste() {
        responder(400, "{\"error\":{\"message\":\"API key not valid\"}}");

        AssistenteIAIndisponivelException e = assertThrows(AssistenteIAIndisponivelException.class,
                () -> cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))));
        assertTrue(e.getMessage().contains("indisponivel"));

        conferirPedidos(1);
    }

    @Test
    @DisplayName("sem chave configurada, falha antes de qualquer chamada")
    void semChaveConfigurada() {
        GeminiClient semChave = clienteCom("");

        assertFalse(semChave.estaConfigurado());
        AssistenteIAIndisponivelException e = assertThrows(AssistenteIAIndisponivelException.class,
                () -> semChave.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))));
        assertTrue(e.getMessage().contains("GEMINI_API_KEY"));
        assertTrue(pedidos.isEmpty());
    }

    @Test
    @DisplayName("ferramenta que falha nao derruba a aplicacao")
    void ferramentaComErro() {
        responder(200, """
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"consulta","args":{}}}]},"finishReason":"STOP"}]}
                """);

        AssistenteIAIndisponivelException e = assertThrows(AssistenteIAIndisponivelException.class,
                () -> cliente.conversar("instrucao",
                        List.of(MensagemIA.doCliente("oi")),
                        List.of(new FerramentaIA("consulta", "consulta", List.of())),
                        (nome, args) -> { throw new IllegalStateException("banco fora do ar"); }));
        assertTrue(e.getMessage().contains("Falha ao consultar dados"));
    }
}
