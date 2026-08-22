package br.com.jence.backend.infrastructure.ia.client;

import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.domain.service.FerramentaIA;
import br.com.jence.backend.domain.service.MensagemIA;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import static org.hamcrest.Matchers.containsString;

/**
 * Testa o cliente sem rede, com respostas simuladas no formato real da API do Gemini
 * (confirmado por chamadas de verdade durante a implementacao).
 */
class GeminiClientTest {

    private RestClient.Builder builder;
    private MockRestServiceServer servidor;
    private GeminiClient cliente;

    private static final String BASE = "https://fake-gemini.test";
    private static final String URL = BASE + "/v1beta/models/modelo-teste:generateContent?key=chave-teste";

    @BeforeEach
    void preparar() {
        builder = RestClient.builder();
        servidor = MockRestServiceServer.bindTo(builder).build();
        cliente = new GeminiClient(builder, BASE, "modelo-teste", "chave-teste");
    }

    @Test
    @DisplayName("extrai o texto de uma resposta simples")
    void respostaDeTexto() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"A lixa grao 150 serve como substituto."}]},"finishReason":"STOP"}]}
                """, MediaType.APPLICATION_JSON));

        String resposta = cliente.conversar("instrucao", List.of(MensagemIA.doCliente("e ai?")));

        assertThat(resposta).isEqualTo("A lixa grao 150 serve como substituto.");
        servidor.verify();
    }

    @Test
    @DisplayName("executa a ferramenta pedida e devolve o resultado ao modelo")
    void cicloDeFerramenta() {
        // 1a resposta: o modelo pede a ferramenta
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"buscar_substitutos","args":{"corredor":"Tintas"},"id":"call_1"},
                   "thoughtSignature":"assinatura-do-provedor"}]},"finishReason":"STOP"}]}
                """, MediaType.APPLICATION_JSON));

        // 2a resposta: com o resultado em maos, ele conclui
        servidor.expect(requestTo(URL))
                .andExpect(content().string(containsString("functionResponse")))
                .andExpect(content().string(containsString("call_1")))
                .andExpect(content().string(containsString("thoughtSignature")))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"role":"model","parts":[
                          {"text":"Leve a Lixa d'Agua Grao 150, no mesmo corredor."}]},"finishReason":"STOP"}]}
                        """, MediaType.APPLICATION_JSON));

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

        assertThat(ferramentaChamada.get()).isEqualTo("buscar_substitutos");
        assertThat(argumentosRecebidos.get()).containsEntry("corredor", "Tintas");
        assertThat(resposta).contains("Lixa d'Agua Grao 150");
        servidor.verify();
    }

    @Test
    @DisplayName("cota estourada e tentada de novo antes de desistir")
    void erroDoProvedor() {
        // O tier gratuito estoura cota com facilidade, e a falha costuma passar em segundos.
        servidor.expect(times(3), requestTo(URL)).andRespond(withTooManyRequests());

        assertThatThrownBy(() -> cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))))
                .isInstanceOf(AssistenteIAIndisponivelException.class)
                .hasMessageContaining("indisponivel");

        servidor.verify();
    }

    @Test
    @DisplayName("falha transitoria seguida de sucesso devolve a resposta")
    void recuperaDeFalhaTransitoria() {
        servidor.expect(requestTo(URL)).andRespond(withServerError());
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"role":"model","parts":[
                  {"text":"Resposta apos o provedor se recuperar."}]},"finishReason":"STOP"}]}
                """, MediaType.APPLICATION_JSON));

        String resposta = cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi")));

        assertThat(resposta).isEqualTo("Resposta apos o provedor se recuperar.");
        servidor.verify();
    }

    @Test
    @DisplayName("resposta sem candidatos (filtro de conteudo) vira indisponibilidade")
    void respostaBloqueada() {
        servidor.expect(requestTo(URL))
                .andRespond(withSuccess("""
                        {"promptFeedback":{"blockReason":"SAFETY"}}
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))))
                .isInstanceOf(AssistenteIAIndisponivelException.class)
                .hasMessageContaining("nao produziu resposta");
    }

    @Test
    @DisplayName("sem chave configurada, falha antes de qualquer chamada")
    void semChaveConfigurada() {
        GeminiClient semChave = new GeminiClient(RestClient.builder(), BASE, "modelo-teste", "");

        assertThat(semChave.estaConfigurado()).isFalse();
        assertThatThrownBy(() -> semChave.conversar("instrucao", List.of(MensagemIA.doCliente("oi"))))
                .isInstanceOf(AssistenteIAIndisponivelException.class)
                .hasMessageContaining("GEMINI_API_KEY");
    }

    @Test
    @DisplayName("ferramenta que falha nao derruba a aplicacao")
    void ferramentaComErro() {
        servidor.expect(requestTo(URL)).andRespond(withSuccess("""
                {"candidates":[{"content":{"role":"model","parts":[
                  {"functionCall":{"name":"consulta","args":{}}}]},"finishReason":"STOP"}]}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> cliente.conversar("instrucao",
                List.of(MensagemIA.doCliente("oi")),
                List.of(new FerramentaIA("consulta", "consulta", List.of())),
                (nome, args) -> { throw new IllegalStateException("banco fora do ar"); }))
                .isInstanceOf(AssistenteIAIndisponivelException.class)
                .hasMessageContaining("Falha ao consultar dados");
    }
}
