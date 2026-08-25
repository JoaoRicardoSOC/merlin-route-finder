package br.com.jence.backend;

import br.com.jence.backend.infrastructure.database.repository.ChatMensagemJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.RegistroRupturaJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A jornada do cliente percorrida por HTTP, da entrada na loja ao encerramento.
 * <p>
 * Ate ser escrito, esse percurso so tinha sido verificado a mao, com {@code curl} digitado
 * durante o desenvolvimento - o que provava que funcionava naquele momento, na maquina de uma
 * pessoa, e nao deixava nada para tras. Aqui ele vira ativo: roda em qualquer maquina com
 * banco, e quebra se alguem desalinhar dois passos que os testes de unidade veem separados.
 * <p>
 * <b>E o nivel que importa para a integracao com o frontend.</b> Os testes de {@code @WebMvcTest}
 * exercitam um controller com casos de uso simulados; aqui sobe a aplicacao inteira e as
 * chamadas passam por serializacao, status HTTP, tratamento de erro e banco real - exatamente
 * o que o celular do cliente vai encontrar.
 * <p>
 * Nao exige {@code GEMINI_API_KEY}: o passo da ruptura aceita tanto a escolha do assistente
 * quanto o substituto por proximidade, que e o fallback da D-38. Assim o teste roda para
 * qualquer integrante com credencial de banco.
 */
@Tag("integracao")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JornadaCompletaIntegracaoTest {

    @LocalServerPort int porta;
    @Autowired RestClient.Builder builder;

    /* Leitor proprio: o teste so precisa navegar o JSON da resposta, e nao depender da
     * configuracao de serializacao da aplicacao - que e justamente uma das coisas que
     * ele existe para verificar. */
    private final ObjectMapper json = new ObjectMapper();

    @Autowired SessaoJpaRepository sessaoJpa;
    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired ChatMensagemJpaRepository chatJpa;
    @Autowired RegistroRupturaJpaRepository rupturaJpa;

    private final List<UUID> sessoesCriadas = new ArrayList<>();

    @AfterEach
    void limpar() {
        sessoesCriadas.forEach(id -> {
            rupturaJpa.findBySessaoIdOrderByRegistradoEmDesc(id).forEach(rupturaJpa::delete);
            chatJpa.findBySessaoIdOrderByEnviadoEmAsc(id).forEach(chatJpa::delete);
            listaJpa.findBySessaoId(id).ifPresent(listaJpa::delete);
            sessaoJpa.deleteById(id);
        });
        sessoesCriadas.clear();
    }

    // ---------------------------------------------------------------- ferramentas HTTP

    /** Nunca lanca: o status faz parte do que se quer verificar. */
    private ResponseEntity<String> chamar(HttpMethod metodo, String caminho, String corpo) {
        RestClient.RequestBodySpec pedido = builder.build()
                .method(metodo)
                .uri("http://localhost:" + porta + "/api/v1" + caminho);

        if (corpo != null) {
            pedido.contentType(MediaType.APPLICATION_JSON).body(corpo);
        }

        return pedido.retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toEntity(String.class);
    }

    private JsonNode corpoDe(ResponseEntity<String> resposta) {
        try {
            return json.readTree(resposta.getBody());
        } catch (Exception e) {
            throw new AssertionError("resposta nao era JSON: " + resposta.getBody(), e);
        }
    }

    private JsonNode get(String caminho) {
        ResponseEntity<String> r = chamar(HttpMethod.GET, caminho, null);
        assertThat(r.getStatusCode().value()).as("GET %s", caminho).isEqualTo(200);
        return corpoDe(r);
    }

    private void passo(String descricao) {
        System.out.println(">>> " + descricao);
    }

    // ---------------------------------------------------------------- montagem

    private UUID novaSessao() {
        ResponseEntity<String> r = chamar(HttpMethod.POST, "/sessoes", null);
        assertThat(r.getStatusCode().value()).isEqualTo(201);

        UUID id = UUID.fromString(corpoDe(r).get("id").asText());
        sessoesCriadas.add(id);
        return id;
    }

    private String idDoProduto(String sku, String termoDeBusca) {
        for (JsonNode produto : get("/produtos?query=" + termoDeBusca).get("content")) {
            if (sku.equals(produto.get("sku").asText())) {
                return produto.get("id").asText();
            }
        }
        throw new AssertionError("produto ausente na massa: " + sku);
    }

    private JsonNode adicionar(UUID sessaoId, String produtoId) {
        ResponseEntity<String> r = chamar(HttpMethod.POST, "/sessoes/" + sessaoId + "/roteiro/itens",
                "{\"produtoId\":\"" + produtoId + "\"}");
        assertThat(r.getStatusCode().value()).isEqualTo(201);
        return corpoDe(r);
    }

    // ---------------------------------------------------------------- a jornada

    @Test
    @DisplayName("da entrada ao encerramento: montar a lista, caminhar, coletar e concluir")
    void jornadaDoClienteNaLoja() {
        UUID sessao = novaSessao();
        passo("sessao aberta: " + sessao);

        // --- busca tolerante a erro de digitacao (UC-002)
        JsonNode busca = get("/produtos?query=tnta");
        assertThat(busca.get("totalElements").asInt())
                .as("'tnta' precisa achar 'Tinta': e a busca fuzzy do UC-002")
                .isPositive();
        passo("busca por 'tnta' -> " + busca.get("content").get(0).get("nome").asText());

        // --- monta o carrinho de roteiro (UC-004), de proposito fora de ordem geografica
        adicionar(sessao, idDoProduto("SKU-DEC-001", "Espelho"));
        adicionar(sessao, idDoProduto("SKU-MAT-002", "Cimento"));
        adicionar(sessao, idDoProduto("SKU-JAR-001", "Vaso"));
        String lixaEmFalta = idDoProduto("SKU-TIN-003", "Lixa");
        adicionar(sessao, lixaEmFalta);

        JsonNode roteiro = get("/sessoes/" + sessao + "/roteiro");
        assertThat(roteiro.get("itens")).hasSize(4);
        passo("roteiro montado com 4 itens");

        // --- produto repetido nao vira parada nova (D-18)
        adicionar(sessao, idDoProduto("SKU-JAR-001", "Vaso"));
        assertThat(get("/sessoes/" + sessao + "/roteiro").get("itens"))
                .as("adicionar o mesmo produto duas vezes nao pode criar duas paradas")
                .hasSize(4);

        // --- caminhada: o cliente escolhe por onde ir e marca o que pegou (UC-014)
        passo("itens a coletar:");
        for (JsonNode item : get("/sessoes/" + sessao + "/roteiro").get("itens")) {
            System.out.printf("    %-32s %s%n",
                    item.get("produto").get("nome").asText(),
                    item.get("produto").get("pontoMapaId").asText().substring(0, 8));
        }

        String primeiroItem = get("/sessoes/" + sessao + "/roteiro")
                .get("itens").get(0).get("id").asText();
        ResponseEntity<String> coleta = chamar(HttpMethod.PATCH,
                "/roteiro/itens/" + primeiroItem + "/coletar", null);
        assertThat(coleta.getStatusCode().value()).isEqualTo(200);
        assertThat(corpoDe(coleta).get("coletado").asBoolean()).isTrue();
        passo("primeiro item coletado");

        // --- marcar de novo e idempotente (D-32)
        assertThat(chamar(HttpMethod.PATCH, "/roteiro/itens/" + primeiroItem + "/coletar", null)
                .getStatusCode().value())
                .as("toque duplo ou reenvio da rede nao e erro")
                .isEqualTo(200);

        // --- conclusao da jornada (UC-014)
        ResponseEntity<String> conclusao = chamar(HttpMethod.POST, "/sessoes/" + sessao + "/concluir", null);
        assertThat(conclusao.getStatusCode().value()).isEqualTo(200);
        assertThat(corpoDe(conclusao).get("status").asText()).isEqualTo("COMPLETED");
        passo("jornada concluida");

        // --- sessao encerrada: leitura sim, escrita nao (D-41)
        assertThat(chamar(HttpMethod.GET, "/sessoes/" + sessao + "/chat/mensagens", null)
                .getStatusCode().value())
                .as("historico e registro do que aconteceu; continua legivel")
                .isEqualTo(200);

        assertThat(chamar(HttpMethod.POST, "/sessoes/" + sessao + "/chat/mensagens",
                "{\"conteudo\":\"ainda posso perguntar?\"}").getStatusCode().value())
                .as("escrever numa jornada encerrada e incoerente")
                .isEqualTo(409);
    }

    // ---------------------------------------------------------------- ruptura

    @Test
    @DisplayName("prateleira vazia devolve um substituto de verdade, com estoque e corredor")
    void rupturaDevolveSubstitutoUtil() {
        UUID sessao = novaSessao();
        String item = adicionar(sessao, idDoProduto("SKU-TIN-003", "Lixa")).get("id").asText();

        ResponseEntity<String> resposta = chamar(HttpMethod.POST,
                "/roteiro/itens/" + item + "/ruptura", null);
        assertThat(resposta.getStatusCode().value()).isEqualTo(200);

        JsonNode ruptura = corpoDe(resposta);
        JsonNode sugerido = ruptura.get("produtoSugerido");

        passo("sugerido: " + sugerido.get("nome").asText()
                + " | " + sugerido.get("pontoMapa").get("corredor").asText()
                + " | origem " + ruptura.get("origemSugestao").asText());
        passo("justificativa: " + ruptura.get("justificativa").asText());

        assertThat(sugerido.get("saldoEstoque").asInt())
                .as("sugerir produto sem estoque manda o cliente a outra prateleira vazia")
                .isPositive();
        assertThat(sugerido.get("pontoMapa").get("corredor").asText())
                .as("sem o corredor o cliente nao sabe para onde ir")
                .isNotBlank();
        assertThat(ruptura.get("justificativa").asText()).isNotBlank();

        /*
         * Aceita as duas origens de proposito: sem GEMINI_API_KEY o fluxo cai no substituto
         * por proximidade (D-38), e o teste precisa continuar valendo para quem so tem banco.
         */
        assertThat(ruptura.get("origemSugestao").asText())
                .isIn("ASSISTENTE_IA", "PROXIMIDADE");
    }

    // ---------------------------------------------------------------- recuperacao

    @Test
    @DisplayName("celular que perdeu a aba recupera a lista so com o id da sessao")
    void celularSeRecuperaComOIdDaSessao() {
        UUID sessao = novaSessao();
        adicionar(sessao, idDoProduto("SKU-MAT-002", "Cimento"));
        String lampada = adicionar(sessao, idDoProduto("SKU-ILU-001", "Lampada")).get("id").asText();

        chamar(HttpMethod.PATCH, "/roteiro/itens/" + lampada + "/coletar", null);

        /*
         * O identificador da sessao vive no localStorage do navegador e sobrevive a fechar a
         * aba. Ao voltar, o celular so precisa dele para reencontrar tudo - inclusive o que
         * ja tinha sido coletado.
         */
        JsonNode roteiro = get("/sessoes/" + sessao + "/roteiro");

        passo("recuperacao pelo id da sessao: " + roteiro.get("itens").size() + " itens");

        assertThat(roteiro.get("itens")).hasSize(2);
        assertThat(roteiro.get("itens").findValuesAsText("coletado"))
                .as("o progresso do cliente precisa sobreviver a fechar a aba")
                .contains("true");
    }

    // ---------------------------------------------------------------- ferramenta de demo

    @Test
    @DisplayName("a simulacao de estoque cria e desfaz o cenario de ruptura sob demanda")
    void simulacaoDeEstoqueCriaEDesfazOCenario() {
        String trena = idDoProduto("SKU-FER-002", "Trena");
        int saldoOriginal = get("/produtos/" + trena).get("saldoEstoque").asInt();

        try {
            ResponseEntity<String> zerado = chamar(HttpMethod.PATCH,
                    "/produtos/" + trena + "/estoque", "{\"saldoEstoque\":0}");
            assertThat(zerado.getStatusCode().value()).isEqualTo(200);
            assertThat(corpoDe(zerado).get("saldoEstoque").asInt()).isZero();

            UUID sessao = novaSessao();
            String item = adicionar(sessao, trena).get("id").asText();

            JsonNode ruptura = corpoDe(chamar(HttpMethod.POST,
                    "/roteiro/itens/" + item + "/ruptura", null));

            passo("cenario criado sob demanda -> " + ruptura.get("produtoSugerido").get("nome").asText());

            assertThat(ruptura.get("produtoSugerido").get("saldoEstoque").asInt()).isPositive();

        } finally {
            // Sem isto, o proximo teste - e a proxima demonstracao - encontraria o catalogo torto.
            chamar(HttpMethod.PATCH, "/produtos/" + trena + "/estoque",
                    "{\"saldoEstoque\":" + saldoOriginal + "}");
        }
    }
}
