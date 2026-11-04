package br.com.jence.backend.ia;

import br.com.jence.backend.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.model.FerramentaIA;
import br.com.jence.backend.model.MensagemIA;
import br.com.jence.backend.model.PapelIA;
import br.com.jence.backend.config.Configuracao;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * O assistente de IA, sobre a API REST do Google Gemini. Unica classe do projeto que conhece o
 * protocolo do provedor.
 * <p>
 * O formato das mensagens foi confirmado por chamadas reais a API, nao assumido: o modelo
 * responde com {@code functionCall{name, args, id}}, e o turno dele precisa ser devolvido
 * <b>inteiro</b> no historico da chamada seguinte.
 */
@Component
public class GeminiClient {

    /** Limite de idas e voltas com o modelo, para uma conversa nunca virar laco infinito. */
    private static final int MAXIMO_DE_CICLOS = 5;

    /** Tentativas por chamada, para absorver falhas transitorias do tier gratuito. */
    private static final int MAXIMO_DE_TENTATIVAS = 3;
    private static final Duration ESPERA_ENTRE_TENTATIVAS = Duration.ofSeconds(2);

    /** Converte o corpo da chamada em JSON e a resposta de volta em mapas e listas. */
    private static final Jsonb JSON = JsonbBuilder.create();

    /*
     * Um cliente HTTP so, reaproveitado entre chamadas, com os tempos limite visiveis aqui. Numa
     * demonstracao ao vivo, esperar por uma API travada e pior do que degradar rapido: o caso de
     * uso tem fallback, mas so se a chamada devolver.
     */
    private final HttpClient http;
    private final Duration tempoLimiteDeLeitura;
    private final String urlBase;
    private final String modelo;
    private final String chave;

    /** Le a configuracao das variaveis de ambiente. */
    public GeminiClient() {
        this(Configuracao.doAmbiente());
    }

    public GeminiClient(Configuracao configuracao) {
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(configuracao.getHttpTempoLimiteConexaoSegundos()))
                .build();
        this.tempoLimiteDeLeitura = Duration.ofSeconds(configuracao.getHttpTempoLimiteLeituraSegundos());
        this.urlBase = configuracao.getIaUrlBase();
        this.modelo = configuracao.getIaModelo();
        this.chave = configuracao.getIaChave();
    }

    public boolean estaConfigurado() {
        return chave != null && !chave.isBlank();
    }

    /** Conversa simples, sem ferramentas. */
    public String conversar(String instrucaoSistema, List<MensagemIA> historico) throws AssistenteIAIndisponivelException {
        return conversar(instrucaoSistema, historico, List.of(), (nome, argumentos) -> Map.of());
    }

    /**
     * Conversa em que o assistente pode pedir a execucao de ferramentas antes de responder.
     * <p>
     * O ciclo de ida e volta com o provedor e resolvido aqui: quem chama apenas informa como
     * executar cada ferramenta.
     *
     * @param instrucaoSistema como o assistente deve se comportar
     * @param historico        a conversa ate aqui
     * @param ferramentas      o que ele pode consultar (vazio para conversa simples)
     * @param executor         executa a ferramenta pedida e devolve o resultado
     * @return o texto final da resposta
     * @throws AssistenteIAIndisponivelException em qualquer falha; o fallback e decisao de
     *                                           quem chama, nao do assistente
     */
    public String conversar(String instrucaoSistema,
                            List<MensagemIA> historico,
                            List<FerramentaIA> ferramentas,
                            ExecutorDeFerramenta executor) throws AssistenteIAIndisponivelException {

        if (!estaConfigurado()) {
            throw new AssistenteIAIndisponivelException(
                    "GEMINI_API_KEY nao configurada: assistente de IA indisponivel");
        }

        List<Map<String, Object>> conversa = new ArrayList<>();
        historico.forEach(m -> conversa.add(turnoDeTexto(papelDaApi(m.papel()), m.conteudo())));

        for (int ciclo = 0; ciclo < MAXIMO_DE_CICLOS; ciclo++) {
            Map<String, Object> resposta = chamar(instrucaoSistema, conversa, ferramentas);
            Map<String, Object> turnoDoModelo = extrairTurnoDoModelo(resposta);
            List<Map<String, Object>> partes = partesDe(turnoDoModelo);

            Map<String, Object> chamadaDeFerramenta = primeiraChamadaDeFerramenta(partes);
            if (chamadaDeFerramenta == null) {
                return extrairTexto(partes);
            }

            /*
             * O turno do modelo volta inteiro, sem reconstrucao: ele carrega campos proprios
             * do provedor (assinatura de raciocinio, por exemplo) que precisam ser
             * preservados para a continuidade da conversa.
             */
            conversa.add(turnoDoModelo);
            conversa.add(turnoDeResultado(chamadaDeFerramenta, executor));
        }

        throw new AssistenteIAIndisponivelException(
                "Assistente excedeu %d ciclos de consulta sem concluir".formatted(MAXIMO_DE_CICLOS));
    }

    // ---------------------------------------------------------------- chamada HTTP

    @SuppressWarnings("unchecked")
    private Map<String, Object> chamar(String instrucaoSistema,
                                       List<Map<String, Object>> conversa,
                                       List<FerramentaIA> ferramentas) throws AssistenteIAIndisponivelException {
        Map<String, Object> corpo = new LinkedHashMap<>();
        if (instrucaoSistema != null && !instrucaoSistema.isBlank()) {
            corpo.put("system_instruction", Map.of("parts", List.of(Map.of("text", instrucaoSistema))));
        }
        corpo.put("contents", conversa);
        if (ferramentas != null && !ferramentas.isEmpty()) {
            corpo.put("tools", List.of(Map.of("function_declarations", declaracoesDe(ferramentas))));
        }

        /*
         * O tier gratuito falha de forma transitoria com frequencia: cota por minuto estourada
         * (429) e picos de demanda do proprio provedor (5xx). Ambos foram observados em uma
         * unica sessao de testes. Sem esta tentativa extra, a demonstracao mostraria a
         * mensagem de indisponibilidade por uma falha que se resolve sozinha em segundos.
         */
        Exception ultimaFalha = null;

        HttpRequest pedido = HttpRequest.newBuilder()
                .uri(URI.create(urlBase + "/v1beta/models/" + modelo + ":generateContent?key="
                        + URLEncoder.encode(chave, StandardCharsets.UTF_8)))
                .timeout(tempoLimiteDeLeitura)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(JSON.toJson(corpo), StandardCharsets.UTF_8))
                .build();

        for (int tentativa = 1; tentativa <= MAXIMO_DE_TENTATIVAS; tentativa++) {
            try {
                HttpResponse<String> resposta = http.send(pedido, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                int status = resposta.statusCode();

                if (status == 429 || status >= 500) {
                    ultimaFalha = new IOException("HTTP " + status + ": " + resposta.body());
                    System.out.println(String.format("AVISO: Assistente indisponivel na tentativa %s de %s: HTTP %s",
                            tentativa, MAXIMO_DE_TENTATIVAS, status));
                    esperarAntesDeInsistir(tentativa);
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new IOException("HTTP " + status + ": " + resposta.body());
                }

                Map<String, Object> corpoDaResposta = resposta.body() == null || resposta.body().isBlank()
                        ? null
                        : JSON.fromJson(resposta.body(), Map.class);
                if (corpoDaResposta == null) {
                    throw new AssistenteIAIndisponivelException("Assistente devolveu resposta vazia");
                }
                return corpoDaResposta;

            } catch (AssistenteIAIndisponivelException e) {
                throw e;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssistenteIAIndisponivelException("Consulta ao assistente interrompida", e);
            } catch (Exception e) {
                // Falhas que nao se resolvem sozinhas (rede, formato) nao merecem nova tentativa.
                System.out.println(String.format("AVISO: Falha ao consultar o assistente de IA: %s", e.toString()));
                throw new AssistenteIAIndisponivelException("Assistente de IA indisponivel no momento", e);
            }
        }

        // Mensagem generica para fora, detalhe no log: o motivo da falha do provedor nao
        // interessa ao cliente final, mas interessa muito a quem for investigar.
        throw new AssistenteIAIndisponivelException("Assistente de IA indisponivel no momento", ultimaFalha);
    }

    private void esperarAntesDeInsistir(int tentativa) throws AssistenteIAIndisponivelException {
        if (tentativa >= MAXIMO_DE_TENTATIVAS) {
            return;
        }
        try {
            Thread.sleep(ESPERA_ENTRE_TENTATIVAS.toMillis() * tentativa);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssistenteIAIndisponivelException("Consulta ao assistente interrompida", e);
        }
    }

    private List<Map<String, Object>> declaracoesDe(List<FerramentaIA> ferramentas) {
        return ferramentas.stream().map(f -> {
            Map<String, Object> propriedades = new LinkedHashMap<>();
            List<String> obrigatorios = new ArrayList<>();

            for (FerramentaIA.ParametroIA p : f.parametros()) {
                propriedades.put(p.nome(), Map.of("type", "string", "description", p.descricao()));
                if (p.obrigatorio()) {
                    obrigatorios.add(p.nome());
                }
            }

            return Map.<String, Object>of(
                    "name", f.nome(),
                    "description", f.descricao(),
                    "parameters", Map.of(
                            "type", "object",
                            "properties", propriedades,
                            "required", obrigatorios));
        }).toList();
    }

    // ---------------------------------------------------------------- leitura da resposta

    @SuppressWarnings("unchecked")
    private Map<String, Object> extrairTurnoDoModelo(Map<String, Object> resposta) throws AssistenteIAIndisponivelException {
        List<Map<String, Object>> candidatos = (List<Map<String, Object>>) resposta.get("candidates");

        if (candidatos == null || candidatos.isEmpty()) {
            // Acontece quando o filtro de conteudo do provedor bloqueia a resposta.
            System.out.println(String.format("AVISO: Assistente respondeu sem candidatos: %s", resposta));
            throw new AssistenteIAIndisponivelException("Assistente nao produziu resposta");
        }

        Map<String, Object> conteudo = (Map<String, Object>) candidatos.get(0).get("content");
        if (conteudo == null) {
            throw new AssistenteIAIndisponivelException("Assistente respondeu em formato inesperado");
        }
        return conteudo;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> partesDe(Map<String, Object> turno) {
        List<Map<String, Object>> partes = (List<Map<String, Object>>) turno.get("parts");
        return partes == null ? List.of() : partes;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> primeiraChamadaDeFerramenta(List<Map<String, Object>> partes) {
        return partes.stream()
                .map(p -> (Map<String, Object>) p.get("functionCall"))
                .filter(java.util.Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    private String extrairTexto(List<Map<String, Object>> partes) throws AssistenteIAIndisponivelException {
        String texto = partes.stream()
                .map(p -> (String) p.get("text"))
                .filter(t -> t != null && !t.isBlank())
                .reduce("", (a, b) -> a.isEmpty() ? b : a + "\n" + b);

        if (texto.isBlank()) {
            throw new AssistenteIAIndisponivelException("Assistente respondeu sem texto");
        }
        return texto;
    }

    // ---------------------------------------------------------------- montagem de turnos

    private Map<String, Object> turnoDeTexto(String papel, String texto) {
        return Map.of("role", papel, "parts", List.of(Map.of("text", texto)));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> turnoDeResultado(Map<String, Object> chamada, ExecutorDeFerramenta executor) throws AssistenteIAIndisponivelException {
        String nome = (String) chamada.get("name");
        Map<String, Object> argumentos = (Map<String, Object>) chamada.getOrDefault("args", Map.of());

        Map<String, Object> resultado;
        try {
            resultado = executor.executar(nome, argumentos);
        } catch (Exception e) {
            System.out.println(String.format("AVISO: Ferramenta '%s' falhou: %s", nome, e.toString()));
            throw new AssistenteIAIndisponivelException("Falha ao consultar dados para o assistente", e);
        }

        Map<String, Object> resposta = new LinkedHashMap<>();
        resposta.put("name", nome);
        resposta.put("response", resultado == null ? Map.of() : resultado);
        // O id so aparece em versoes mais novas da API; quando vier, precisa ser ecoado.
        if (chamada.get("id") != null) {
            resposta.put("id", chamada.get("id"));
        }

        return Map.of("role", "user", "parts", List.of(Map.of("functionResponse", resposta)));
    }

    private String papelDaApi(PapelIA papel) {
        return papel == PapelIA.ASSISTENTE ? "model" : "user";
    }
}
