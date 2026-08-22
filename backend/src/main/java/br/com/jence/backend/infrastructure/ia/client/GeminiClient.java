package br.com.jence.backend.infrastructure.ia.client;

import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.ExecutorDeFerramenta;
import br.com.jence.backend.domain.service.FerramentaIA;
import br.com.jence.backend.domain.service.MensagemIA;
import br.com.jence.backend.domain.service.PapelIA;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementacao do assistente sobre a API REST do Google Gemini. Unica classe do projeto que
 * conhece o protocolo do provedor.
 * <p>
 * O formato das mensagens foi confirmado por chamadas reais a API, nao assumido: o modelo
 * responde com {@code functionCall{name, args, id}}, e o turno dele precisa ser devolvido
 * <b>inteiro</b> no historico da chamada seguinte.
 */
@Component
@Slf4j
public class GeminiClient implements AssistenteIA {

    /** Limite de idas e voltas com o modelo, para uma conversa nunca virar laco infinito. */
    private static final int MAXIMO_DE_CICLOS = 5;

    /** Tentativas por chamada, para absorver falhas transitorias do tier gratuito. */
    private static final int MAXIMO_DE_TENTATIVAS = 3;
    private static final Duration ESPERA_ENTRE_TENTATIVAS = Duration.ofSeconds(2);

    private final RestClient restClient;
    private final String modelo;
    private final String chave;

    public GeminiClient(RestClient.Builder builder,
                        @Value("${merlin.ia.base-url}") String baseUrl,
                        @Value("${merlin.ia.modelo}") String modelo,
                        @Value("${merlin.ia.api-key:}") String chave) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.modelo = modelo;
        this.chave = chave;
    }

    public boolean estaConfigurado() {
        return chave != null && !chave.isBlank();
    }

    @Override
    public String conversar(String instrucaoSistema,
                            List<MensagemIA> historico,
                            List<FerramentaIA> ferramentas,
                            ExecutorDeFerramenta executor) {

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
                                       List<FerramentaIA> ferramentas) {
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

        for (int tentativa = 1; tentativa <= MAXIMO_DE_TENTATIVAS; tentativa++) {
            try {
                Map<String, Object> resposta = restClient.post()
                        .uri("/v1beta/models/{modelo}:generateContent?key={chave}", modelo, chave)
                        .body(corpo)
                        .retrieve()
                        .body(Map.class);

                if (resposta == null) {
                    throw new AssistenteIAIndisponivelException("Assistente devolveu resposta vazia");
                }
                return resposta;

            } catch (AssistenteIAIndisponivelException e) {
                throw e;
            } catch (HttpClientErrorException.TooManyRequests | HttpServerErrorException e) {
                ultimaFalha = e;
                log.warn("Assistente indisponivel na tentativa {} de {}: {}",
                        tentativa, MAXIMO_DE_TENTATIVAS, e.getMessage());
                esperarAntesDeInsistir(tentativa);
            } catch (Exception e) {
                // Falhas que nao se resolvem sozinhas (rede, formato) nao merecem nova tentativa.
                log.warn("Falha ao consultar o assistente de IA: {}", e.toString());
                throw new AssistenteIAIndisponivelException("Assistente de IA indisponivel no momento", e);
            }
        }

        // Mensagem generica para fora, detalhe no log: o motivo da falha do provedor nao
        // interessa ao cliente final, mas interessa muito a quem for investigar.
        throw new AssistenteIAIndisponivelException("Assistente de IA indisponivel no momento", ultimaFalha);
    }

    private void esperarAntesDeInsistir(int tentativa) {
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
    private Map<String, Object> extrairTurnoDoModelo(Map<String, Object> resposta) {
        List<Map<String, Object>> candidatos = (List<Map<String, Object>>) resposta.get("candidates");

        if (candidatos == null || candidatos.isEmpty()) {
            // Acontece quando o filtro de conteudo do provedor bloqueia a resposta.
            log.warn("Assistente respondeu sem candidatos: {}", resposta);
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

    private String extrairTexto(List<Map<String, Object>> partes) {
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
    private Map<String, Object> turnoDeResultado(Map<String, Object> chamada, ExecutorDeFerramenta executor) {
        String nome = (String) chamada.get("name");
        Map<String, Object> argumentos = (Map<String, Object>) chamada.getOrDefault("args", Map.of());

        Map<String, Object> resultado;
        try {
            resultado = executor.executar(nome, argumentos);
        } catch (Exception e) {
            log.warn("Ferramenta '{}' falhou: {}", nome, e.toString());
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
