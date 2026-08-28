package br.com.jence.backend;

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

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Todo erro da API, visto como o frontend vai ve-lo.
 * <p>
 * <b>O que se verifica aqui nao e cada mensagem, e sim a forma.</b> Se um endpoint devolver um
 * erro com formato diferente dos outros, o frontend precisa de um tratamento por endpoint em
 * vez de um so - e vai descobrir isso na integracao, um caso de cada vez.
 * <p>
 * Cada resposta de erro precisa: ser {@code application/json}, trazer os cinco campos do
 * contrato, ter o {@code status} igual ao codigo HTTP, apontar o {@code path} que foi chamado,
 * e <b>nunca vazar detalhe interno</b> - nome de classe, pacote ou pilha.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RespostasDeErroIntegracaoTest {

    @LocalServerPort int porta;
    @Autowired RestClient.Builder builder;

    @Autowired SessaoJpaRepository sessaoJpa;
    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired RegistroRupturaJpaRepository rupturaJpa;

    private final ObjectMapper json = new ObjectMapper();
    private final List<UUID> sessoesCriadas = new ArrayList<>();

    @AfterEach
    void limpar() {
        sessoesCriadas.forEach(id -> {
            rupturaJpa.findBySessaoIdOrderByRegistradoEmDesc(id).forEach(rupturaJpa::delete);
            listaJpa.findBySessaoId(id).ifPresent(listaJpa::delete);
            sessaoJpa.deleteById(id);
        });
        sessoesCriadas.clear();
    }

    // ---------------------------------------------------------------- ferramentas

    private ResponseEntity<String> chamar(HttpMethod metodo, String caminho, String corpo,
                                          MediaType tipo) {
        RestClient.RequestBodySpec pedido = builder.build()
                .method(metodo)
                .uri("http://localhost:" + porta + caminho);

        if (corpo != null) {
            pedido.contentType(tipo == null ? MediaType.APPLICATION_JSON : tipo).body(corpo);
        }
        return pedido.retrieve().onStatus(status -> true, (req, res) -> { }).toEntity(String.class);
    }

    private ResponseEntity<String> chamar(HttpMethod metodo, String caminho, String corpo) {
        return chamar(metodo, caminho, corpo, null);
    }

    private JsonNode corpoDe(ResponseEntity<String> resposta) {
        try {
            return json.readTree(resposta.getBody());
        } catch (Exception e) {
            throw new AssertionError("resposta nao era JSON: " + resposta.getBody(), e);
        }
    }

    /**
     * A verificacao central: um erro so esta certo se tiver a forma que todos os outros tem.
     */
    private JsonNode erroBemFormado(ResponseEntity<String> resposta, int statusEsperado,
                                    String caminhoChamado) {
        assertThat(resposta.getStatusCode().value())
                .as("codigo HTTP de %s", caminhoChamado)
                .isEqualTo(statusEsperado);

        assertThat(resposta.getHeaders().getContentType())
                .as("erro precisa ser JSON, senao o frontend nem consegue ler a mensagem")
                .isNotNull()
                .satisfies(tipo -> assertThat(tipo.includes(MediaType.APPLICATION_JSON)).isTrue());

        JsonNode corpo = corpoDe(resposta);

        assertThat(corpo.has("timestamp")).as("campo timestamp em %s", caminhoChamado).isTrue();
        assertThat(corpo.has("status")).as("campo status em %s", caminhoChamado).isTrue();
        assertThat(corpo.has("error")).as("campo error em %s", caminhoChamado).isTrue();
        assertThat(corpo.has("message")).as("campo message em %s", caminhoChamado).isTrue();
        assertThat(corpo.has("path")).as("campo path em %s", caminhoChamado).isTrue();

        assertThat(corpo.get("status").asInt())
                .as("o status do corpo tem que bater com o codigo HTTP em %s", caminhoChamado)
                .isEqualTo(statusEsperado);

        assertThat(corpo.get("error").asText()).as("error em %s", caminhoChamado).isNotBlank();
        assertThat(corpo.get("message").asText()).as("message em %s", caminhoChamado).isNotBlank();

        assertThat(corpo.get("path").asText())
                .as("o path precisa dizer qual chamada falhou, ou o log do frontend fica cego")
                .isEqualTo(caminhoChamado);

        String tudo = resposta.getBody();
        assertThat(tudo)
                .as("erro nao pode vazar detalhe interno em %s", caminhoChamado)
                .doesNotContain("br.com.jence")
                .doesNotContain("org.springframework")
                .doesNotContain("java.lang")
                .doesNotContain("Exception")
                .doesNotContain("at ");

        return corpo;
    }

    private UUID sessaoAberta() {
        ResponseEntity<String> r = chamar(HttpMethod.POST, "/api/v1/sessoes", null);
        UUID id = UUID.fromString(corpoDe(r).get("id").asText());
        sessoesCriadas.add(id);
        return id;
    }

    private String algumProdutoDe(String secao) {
        return corpoDe(chamar(HttpMethod.GET,
                "/api/v1/produtos?size=1&secao=" + secao.replace(" ", "%20"), null))
                .get("content").get(0).get("id").asText();
    }

    // ---------------------------------------------------------------- 400

    @Test
    @DisplayName("400 de validacao traz a lista de campos, e so ele traz")
    void validacao() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";

        JsonNode erro = erroBemFormado(chamar(HttpMethod.POST, caminho, "{}"), 400, caminho);

        assertThat(erro.get("validationErrors").isArray()).isTrue();
        assertThat(erro.get("validationErrors")).isNotEmpty();
        assertThat(erro.get("validationErrors").get(0).has("field")).isTrue();
        assertThat(erro.get("validationErrors").get(0).has("message")).isTrue();
    }

    @Test
    @DisplayName("400 de JSON malformado nao traz lista de campos, porque nao ha campo a apontar")
    void jsonMalformado() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";

        JsonNode erro = erroBemFormado(
                chamar(HttpMethod.POST, caminho, "{\"produtoId\": "), 400, caminho);

        assertThat(erro.get("validationErrors") == null || erro.get("validationErrors").isNull())
                .as("lista de campos so faz sentido quando o corpo pode ser lido")
                .isTrue();
    }

    @Test
    @DisplayName("400 de identificador malformado na URL")
    void idMalformado() {
        String caminho = "/api/v1/produtos/nao-e-um-uuid";
        JsonNode erro = erroBemFormado(chamar(HttpMethod.GET, caminho, null), 400, caminho);

        assertThat(erro.get("message").asText())
                .as("a mensagem precisa dizer qual valor foi recusado")
                .contains("nao-e-um-uuid");
    }

    @Test
    @DisplayName("400 quando a query string nao e texto valido, e nao 500")
    void queryIndecodificavel() {
        /*
         * %E2 e o "â" em latin-1, e nao forma um caractere valido em UTF-8. Um cliente que
         * codifique errado cai aqui. Antes a resposta era 500 com "Ocorreu um erro
         * inesperado" - jogava no cliente a impressao de defeito no servidor, e ainda escondia
         * a causa. E erro do pedido.
         *
         * A URI vai como objeto, e nao como String: o RestClient trata String como template e
         * escaparia o proprio sinal de porcentagem, transformando %E2 em %25E2 - que e texto
         * valido e nao reproduziria nada. Foi assim que este teste falhou na primeira tentativa.
         *
         * A excecao nasce no Tomcat, antes de qualquer controlador existir, e por isso a
         * auditoria de respostas de erro nao a tinha alcancado. Ver D-74.
         */
        URI uri = URI.create("http://localhost:" + porta + "/api/v1/produtos?query=L%E2mpada");

        ResponseEntity<String> resposta = builder.build()
                .method(HttpMethod.GET).uri(uri)
                .retrieve().onStatus(status -> true, (req, res) -> { })
                .toEntity(String.class);

        assertThat(resposta.getStatusCode().value())
                .as("parametro mal codificado e erro do cliente, nao do servidor")
                .isEqualTo(400);

        JsonNode erro = corpoDe(resposta);
        assertThat(erro.get("status").asInt()).isEqualTo(400);
        assertThat(erro.get("path").asText()).isEqualTo("/api/v1/produtos");
        assertThat(erro.get("message").asText())
                .as("a mensagem precisa apontar a codificacao, que e onde esta o problema")
                .containsIgnoringCase("UTF-8");
    }

    // ---------------------------------------------------------------- 404

    @Test
    @DisplayName("404 de recurso inexistente")
    void recursoInexistente() {
        String caminho = "/api/v1/produtos/" + UUID.randomUUID();
        erroBemFormado(chamar(HttpMethod.GET, caminho, null), 404, caminho);
    }

    @Test
    @DisplayName("404 de endereco que nao existe na API")
    void enderecoInexistente() {
        // O caso que aparece quando alguem abre a raiz da API no navegador.
        String caminho = "/api/v1/nao-existe";
        erroBemFormado(chamar(HttpMethod.GET, caminho, null), 404, caminho);
    }

    @Test
    @DisplayName("404 ao recentrar com placa desconhecida")
    void placaDesconhecida() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/posicao";

        erroBemFormado(chamar(HttpMethod.PUT, caminho, "{\"codigoPonto\":\"ZZZ-99\"}"), 404, caminho);
    }

    // ---------------------------------------------------------------- 405 e 415

    @Test
    @DisplayName("405 quando o metodo nao e aceito naquele endereco")
    void metodoErrado() {
        String caminho = "/api/v1/mapa";
        erroBemFormado(chamar(HttpMethod.DELETE, caminho, null), 405, caminho);
    }

    @Test
    @DisplayName("415 quando o corpo nao vem como JSON")
    void formatoErrado() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";

        erroBemFormado(
                chamar(HttpMethod.POST, caminho, "produtoId=1", MediaType.TEXT_PLAIN), 415, caminho);
    }

    // ---------------------------------------------------------------- 409

    @Test
    @DisplayName("409 ao escrever numa sessao encerrada")
    void sessaoEncerrada() {
        UUID sessao = sessaoAberta();
        chamar(HttpMethod.POST, "/api/v1/sessoes/" + sessao + "/concluir", null);

        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";
        JsonNode erro = erroBemFormado(chamar(HttpMethod.POST, caminho,
                "{\"produtoId\":\"" + algumProdutoDe("Tintas") + "\"}"), 409, caminho);

        assertThat(erro.get("message").asText())
                .as("a mensagem precisa deixar claro que o problema e o estado, nao o pedido")
                .containsIgnoringCase("ativa");
    }

    @Test
    @DisplayName("409 ao trocar um produto por ele mesmo")
    void substituirPorSiMesmo() {
        UUID sessao = sessaoAberta();
        String produto = algumProdutoDe("Tintas");

        String item = corpoDe(chamar(HttpMethod.POST, "/api/v1/sessoes/" + sessao + "/roteiro/itens",
                "{\"produtoId\":\"" + produto + "\"}")).get("id").asText();

        String caminho = "/api/v1/roteiro/itens/" + item + "/substituir";
        erroBemFormado(chamar(HttpMethod.POST, caminho,
                "{\"produtoSubstitutoId\":\"" + produto + "\"}"), 409, caminho);
    }

    // ---------------------------------------------------------------- 422

    @Test
    @DisplayName("422 quando nao ha substituto possivel, e a ruptura fica registrada mesmo assim")
    void semSubstitutoPossivel() {
        /*
         * Decoracao nao tem vizinho dentro do raio (medido na O-12), entao os unicos candidatos
         * sao os proprios produtos da secao. Colocando todos no roteiro, nao sobra ninguem para
         * sugerir - e o 422 acontece sem depender do assistente.
         */
        UUID sessao = sessaoAberta();

        /*
         * O nome da secao ganhou acento (D-70) e vai cru: o RestClient trata a string como
         * template de URI e codifica sozinho. Codificar antes produz %25C3%25A7 - o sinal de
         * porcentagem escapado de novo -, e a secao nao casa com nada.
         */
        JsonNode decoracao = corpoDe(chamar(HttpMethod.GET,
                "/api/v1/produtos?size=50&secao=Decoração", null)).get("content");

        String itemEmFalta = null;
        for (JsonNode produto : decoracao) {
            String item = corpoDe(chamar(HttpMethod.POST,
                    "/api/v1/sessoes/" + sessao + "/roteiro/itens",
                    "{\"produtoId\":\"" + produto.get("id").asText() + "\"}")).get("id").asText();
            if (itemEmFalta == null) {
                itemEmFalta = item;
                chamar(HttpMethod.PATCH, "/api/v1/produtos/" + produto.get("id").asText()
                        + "/estoque", "{\"saldoEstoque\":0}");
            }
        }

        try {
            String caminho = "/api/v1/roteiro/itens/" + itemEmFalta + "/ruptura";
            JsonNode erro = erroBemFormado(chamar(HttpMethod.POST, caminho, null), 422, caminho);

            assertThat(erro.get("message").asText())
                    .as("o cliente precisa entender que nao ha alternativa, nao que o sistema falhou")
                    .isNotBlank();

            assertThat(rupturaJpa.findBySessaoIdOrderByRegistradoEmDesc(sessao))
                    .as("sem substituto e o relato mais grave: nao pode se perder")
                    .isNotEmpty();

        } finally {
            String primeiro = decoracao.get(0).get("id").asText();
            chamar(HttpMethod.PATCH, "/api/v1/produtos/" + primeiro + "/estoque",
                    "{\"saldoEstoque\":9}");
        }
    }

    // ---------------------------------------------------------------- a forma, vista de cima

    @Test
    @DisplayName("todos os erros da API tem exatamente a mesma forma")
    void formaUnica() {
        /*
         * O teste que resume o proposito deste arquivo. Se um endpoint novo devolver um erro
         * com outro formato, e aqui que aparece - antes de o frontend descobrir na integracao.
         */
        UUID sessao = sessaoAberta();
        String caminho404 = "/api/v1/produtos/" + UUID.randomUUID();
        String caminho400 = "/api/v1/sessoes/" + sessao + "/roteiro/itens";
        String caminho405 = "/api/v1/mapa";

        List<JsonNode> erros = List.of(
                corpoDe(chamar(HttpMethod.GET, caminho404, null)),
                corpoDe(chamar(HttpMethod.POST, caminho400, "{}")),
                corpoDe(chamar(HttpMethod.DELETE, caminho405, null)),
                corpoDe(chamar(HttpMethod.GET, "/api/v1/nao-existe", null)));

        for (JsonNode erro : erros) {
            List<String> campos = new ArrayList<>();
            erro.fieldNames().forEachRemaining(campos::add);

            assertThat(campos)
                    .as("campos de %s", erro.get("path").asText())
                    .containsExactlyInAnyOrder(
                            "timestamp", "status", "error", "message", "path", "validationErrors");
        }
    }
}
