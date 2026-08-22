package br.com.jence.backend;

import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.FerramentaIA;
import br.com.jence.backend.domain.service.MensagemIA;
import br.com.jence.backend.infrastructure.ia.client.GeminiClient;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/** Conversa de verdade com a API do Gemini. Exige GEMINI_API_KEY. */
@Tag("integracao")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class GeminiIntegracaoTest {

    @Autowired AssistenteIA assistente;
    @Autowired GeminiClient cliente;

    @Test
    void respondeEmPortugues() {
        assertThat(cliente.estaConfigurado()).isTrue();

        String resposta = assistente.conversar(
                "Voce atende clientes de uma loja de material de construcao. Responda em portugues, "
                        + "em uma frase curta.",
                List.of(MensagemIA.doCliente("Para que serve uma lixa grao 120?")));

        System.out.println(">>> resposta do Gemini: " + resposta);
        assertThat(resposta).isNotBlank();
    }

    /** A prova de que a fundacao do tratamento de ruptura funciona. */
    @Test
    void executaCicloCompletoDeFerramenta() {
        AtomicReference<Map<String, Object>> argumentos = new AtomicReference<>();

        String resposta = assistente.conversar(
                "Voce ajuda clientes de uma loja de material de construcao. Sempre use a ferramenta "
                        + "para consultar produtos antes de sugerir substituto. Nunca invente produtos. "
                        + "Responda em uma frase curta, citando o nome do produto sugerido.",
                List.of(MensagemIA.doCliente(
                        "Estou no corredor Tintas. A lixa grao 120 acabou na prateleira. Tem substituto perto?")),
                List.of(new FerramentaIA(
                        "buscar_substitutos_proximos",
                        "Busca produtos disponiveis em estoque proximos a um corredor da loja.",
                        List.of(
                                FerramentaIA.ParametroIA.obrigatorio("corredor", "Corredor onde o cliente esta"),
                                FerramentaIA.ParametroIA.opcional("termo", "Tipo de produto procurado")))),
                (nome, args) -> {
                    argumentos.set(args);
                    System.out.println(">>> o modelo pediu '" + nome + "' com " + args);
                    return Map.of("produtos", List.of(
                            Map.of("sku", "SKU-TIN-004", "nome", "Lixa d'Agua Grao 150",
                                    "corredor", "Tintas", "estoque", 40),
                            Map.of("sku", "SKU-TIN-002", "nome", "Rolo de La 23cm",
                                    "corredor", "Tintas", "estoque", 25)));
                });

        System.out.println(">>> resposta final: " + resposta);

        assertThat(argumentos.get())
                .as("o modelo precisa ter chamado a ferramenta")
                .isNotNull()
                .containsKey("corredor");

        assertThat(resposta)
                .as("a sugestao tem que vir dos produtos que nos fornecemos, nao da imaginacao do modelo")
                .containsIgnoringCase("lixa");
    }
}
