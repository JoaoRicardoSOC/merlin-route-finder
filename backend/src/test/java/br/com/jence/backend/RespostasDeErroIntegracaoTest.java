package br.com.jence.backend;

import br.com.jence.backend.resource.ServidorDeTeste;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Todo erro da API, visto como o frontend vai ve-lo.
 * <p>
 * <b>O que se verifica aqui nao e cada mensagem, e sim a forma.</b> Se um endpoint devolver um
 * erro com formato diferente dos outros, o frontend precisa de um tratamento por endpoint em
 * vez de um so - e vai descobrir isso na integracao, um caso de cada vez.
 * <p>
 * Cada resposta de erro tratada pelos Resources precisa: ser {@code application/json}, trazer
 * os cinco campos do contrato, ter o {@code status} igual ao codigo HTTP, apontar o
 * {@code path} que foi chamado, e <b>nunca vazar detalhe interno</b> - nome de classe, pacote
 * ou pilha.
 * <p>
 * Os erros que o Jersey responde antes de chegar a um Resource - metodo nao permitido,
 * {@code Content-Type} errado, endereco inexistente - mantem o status, mas vem sem corpo. Isso
 * e aceito pelo RF-04: o frontend so le o status nesses casos. Para eles, verifica-se o status.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
class RespostasDeErroIntegracaoTest {

    /** A aplicacao de verdade, subida pela Main, com a massa de dados ja carregada. */
    private static ServidorDeTeste servidor;

    private final List<UUID> sessoesCriadas = new ArrayList<>();

    @BeforeAll
    static void subir() throws Exception {
        BancoDeTeste.carregarMassa();
        servidor = ServidorDeTeste.daAplicacao();
    }

    @AfterAll
    static void descer() {
        servidor.close();
    }

    @AfterEach
    void limpar() throws Exception {
        for (UUID id : sessoesCriadas) {
            BancoDeTeste.apagarSessao(id);
        }
        sessoesCriadas.clear();
    }

    // ---------------------------------------------------------------- ferramentas

    /** Nunca lanca: o status faz parte do que se quer verificar. */
    private ServidorDeTeste.Resposta chamar(String metodo, String caminho, String corpo, String tipo) {
        try {
            return servidor.enviar(metodo, caminho,
                    corpo == null ? null : (tipo == null ? "application/json" : tipo), corpo);
        } catch (Exception e) {
            throw new AssertionError("chamada falhou: " + metodo + " " + caminho, e);
        }
    }

    private ServidorDeTeste.Resposta chamar(String metodo, String caminho, String corpo) {
        return chamar(metodo, caminho, corpo, null);
    }

    private NoJson corpoDe(ServidorDeTeste.Resposta resposta) {
        try {
            return NoJson.ler(resposta.corpo());
        } catch (Exception e) {
            throw new AssertionError("resposta nao era JSON: " + resposta.corpo(), e);
        }
    }

    /**
     * A verificacao central: um erro so esta certo se tiver a forma que todos os outros tem.
     */
    private NoJson erroBemFormado(ServidorDeTeste.Resposta resposta, int statusEsperado,
                                  String caminhoChamado) {
        assertEquals(statusEsperado, resposta.status(), "codigo HTTP de " + caminhoChamado);

        String tipo = resposta.tipo().orElse(null);
        assertNotNull(tipo, "erro precisa ser JSON, senao o frontend nem consegue ler a mensagem");
        assertTrue(tipo.startsWith("application/json"),
                "erro precisa ser JSON, senao o frontend nem consegue ler a mensagem");

        NoJson corpo = corpoDe(resposta);

        assertTrue(corpo.has("timestamp"), "campo timestamp em " + caminhoChamado);
        assertTrue(corpo.has("status"), "campo status em " + caminhoChamado);
        assertTrue(corpo.has("error"), "campo error em " + caminhoChamado);
        assertTrue(corpo.has("message"), "campo message em " + caminhoChamado);
        assertTrue(corpo.has("path"), "campo path em " + caminhoChamado);

        assertEquals(statusEsperado, corpo.get("status").asInt(),
                "o status do corpo tem que bater com o codigo HTTP em " + caminhoChamado);

        String error = corpo.get("error").asText();
        assertFalse(error == null || error.isBlank(), "error em " + caminhoChamado);
        String message = corpo.get("message").asText();
        assertFalse(message == null || message.isBlank(), "message em " + caminhoChamado);

        assertEquals(caminhoChamado.replaceAll("\\?.*", ""), corpo.get("path").asText(),
                "o path precisa dizer qual chamada falhou, ou o log do frontend fica cego");

        String tudo = resposta.corpo();
        String semVazamento = "erro nao pode vazar detalhe interno em " + caminhoChamado;
        assertNotNull(tudo, semVazamento);
        assertFalse(tudo.contains("br.com.jence"), semVazamento);
        assertFalse(tudo.contains("org.glassfish"), semVazamento);
        assertFalse(tudo.contains("java.lang"), semVazamento);
        assertFalse(tudo.contains("Exception"), semVazamento);
        assertFalse(tudo.contains("at "), semVazamento);

        return corpo;
    }

    private UUID sessaoAberta() {
        ServidorDeTeste.Resposta r = chamar("POST", "/api/v1/sessoes", null);
        UUID id = UUID.fromString(corpoDe(r).get("id").asText());
        sessoesCriadas.add(id);
        return id;
    }

    private String algumProdutoDe(String secao) {
        return corpoDe(chamar("GET",
                "/api/v1/produtos?size=1&secao=" + secao.replace(" ", "%20"), null))
                .get("content").get(0).get("id").asText();
    }

    // ---------------------------------------------------------------- 400

    @Test
    @DisplayName("400 de validacao traz a lista de campos, e so ele traz")
    void validacao() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";

        NoJson erro = erroBemFormado(chamar("POST", caminho, "{}"), 400, caminho);

        assertTrue(erro.get("validationErrors").isArray());
        assertFalse(erro.get("validationErrors").isEmpty());
        assertTrue(erro.get("validationErrors").get(0).has("field"));
        assertTrue(erro.get("validationErrors").get(0).has("message"));
    }

    @Test
    @DisplayName("400 de JSON malformado nao traz lista de campos, porque nao ha campo a apontar")
    void jsonMalformado() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";

        NoJson erro = erroBemFormado(chamar("POST", caminho, "{\"produtoId\": "), 400, caminho);

        assertTrue(erro.get("validationErrors") == null || erro.get("validationErrors").isNull(),
                "lista de campos so faz sentido quando o corpo pode ser lido");
    }

    @Test
    @DisplayName("400 de identificador malformado na URL")
    void idMalformado() {
        String caminho = "/api/v1/produtos/nao-e-um-uuid";
        NoJson erro = erroBemFormado(chamar("GET", caminho, null), 400, caminho);

        assertTrue(erro.get("message").asText().contains("nao-e-um-uuid"),
                "a mensagem precisa dizer qual valor foi recusado");
    }

    @Test
    @DisplayName("400 quando a query string nao e texto valido, e nao 500")
    void queryIndecodificavel() {
        /*
         * %E2 e o "a" com circunflexo em latin-1, e nao forma um caractere valido em UTF-8. Um
         * cliente que codifique errado cai aqui. E erro do pedido (D-74). O caminho vai cru: o
         * ServidorDeTeste monta a URI sem escapar de novo o sinal de porcentagem.
         */
        String caminho = "/api/v1/produtos?query=L%E2mpada";

        NoJson erro = erroBemFormado(chamar("GET", caminho, null), 400, caminho);

        assertTrue(erro.get("message").asText().toLowerCase().contains("utf-8"),
                "a mensagem precisa apontar a codificacao, que e onde esta o problema");
    }

    // ---------------------------------------------------------------- 404

    @Test
    @DisplayName("404 de recurso inexistente")
    void recursoInexistente() {
        String caminho = "/api/v1/produtos/" + UUID.randomUUID();
        erroBemFormado(chamar("GET", caminho, null), 404, caminho);
    }

    @Test
    @DisplayName("404 de endereco que nao existe na API, respondido pelo Jersey")
    void enderecoInexistente() {
        // O caso que aparece quando alguem abre a raiz da API no navegador.
        assertEquals(404, chamar("GET", "/api/v1/nao-existe", null).status());
    }

    @Test
    @DisplayName("404 ao recentrar com placa desconhecida")
    void placaDesconhecida() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/posicao";

        erroBemFormado(chamar("PUT", caminho, "{\"codigoPonto\":\"ZZZ-99\"}"), 404, caminho);
    }

    // ---------------------------------------------------------------- 405 e 415

    @Test
    @DisplayName("405 quando o metodo nao e aceito naquele endereco, respondido pelo Jersey")
    void metodoErrado() {
        assertEquals(405, chamar("DELETE", "/api/v1/mapa", null).status());
    }

    @Test
    @DisplayName("415 quando o corpo nao vem como JSON, respondido pelo Jersey")
    void formatoErrado() {
        UUID sessao = sessaoAberta();
        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";

        assertEquals(415, chamar("POST", caminho, "produtoId=1", "text/plain").status());
    }

    // ---------------------------------------------------------------- 409

    @Test
    @DisplayName("409 ao escrever numa sessao encerrada")
    void sessaoEncerrada() {
        UUID sessao = sessaoAberta();
        chamar("POST", "/api/v1/sessoes/" + sessao + "/concluir", null);

        String caminho = "/api/v1/sessoes/" + sessao + "/roteiro/itens";
        NoJson erro = erroBemFormado(chamar("POST", caminho,
                "{\"produtoId\":\"" + algumProdutoDe("Tintas") + "\"}"), 409, caminho);

        assertTrue(erro.get("message").asText().toLowerCase().contains("ativa"),
                "a mensagem precisa deixar claro que o problema e o estado, nao o pedido");
    }

    @Test
    @DisplayName("409 ao marcar ou desmarcar a coleta numa sessao encerrada")
    void coletaEmSessaoEncerrada() {
        UUID sessao = sessaoAberta();
        String item = corpoDe(chamar("POST", "/api/v1/sessoes/" + sessao + "/roteiro/itens",
                "{\"produtoId\":\"" + algumProdutoDe("Tintas") + "\"}")).get("id").asText();
        chamar("POST", "/api/v1/sessoes/" + sessao + "/concluir", null);

        for (String acao : new String[] {"coletar", "desmarcar"}) {
            String caminho = "/api/v1/roteiro/itens/" + item + "/" + acao;
            erroBemFormado(chamar("PATCH", caminho, null), 409, caminho);
        }
    }

    @Test
    @DisplayName("409 ao trocar um produto por ele mesmo")
    void substituirPorSiMesmo() {
        UUID sessao = sessaoAberta();
        String produto = algumProdutoDe("Tintas");

        String item = corpoDe(chamar("POST", "/api/v1/sessoes/" + sessao + "/roteiro/itens",
                "{\"produtoId\":\"" + produto + "\"}")).get("id").asText();

        String caminho = "/api/v1/roteiro/itens/" + item + "/substituir";
        erroBemFormado(chamar("POST", caminho,
                "{\"produtoSubstitutoId\":\"" + produto + "\"}"), 409, caminho);
    }

    // ---------------------------------------------------------------- 422

    @Test
    @DisplayName("422 quando nao ha substituto possivel, e a ruptura fica registrada mesmo assim")
    void semSubstitutoPossivel() throws Exception {
        /*
         * Decoracao nao tem vizinho dentro do raio (medido na O-12), entao os unicos candidatos
         * sao os proprios produtos da secao. Colocando todos no roteiro, nao sobra ninguem para
         * sugerir - e o 422 acontece sem depender do assistente.
         */
        UUID sessao = sessaoAberta();

        // O nome da secao tem acento (D-70) e vai ja codificado em UTF-8.
        NoJson decoracao = corpoDe(chamar("GET",
                "/api/v1/produtos?size=50&secao=Decora%C3%A7%C3%A3o", null)).get("content");

        String itemEmFalta = null;
        for (NoJson produto : decoracao) {
            String item = corpoDe(chamar("POST",
                    "/api/v1/sessoes/" + sessao + "/roteiro/itens",
                    "{\"produtoId\":\"" + produto.get("id").asText() + "\"}")).get("id").asText();
            if (itemEmFalta == null) {
                itemEmFalta = item;
                chamar("PATCH", "/api/v1/produtos/" + produto.get("id").asText()
                        + "/estoque", "{\"saldoEstoque\":0}");
            }
        }

        try {
            String caminho = "/api/v1/roteiro/itens/" + itemEmFalta + "/ruptura";
            NoJson erro = erroBemFormado(chamar("POST", caminho, null), 422, caminho);

            String mensagem = erro.get("message").asText();
            assertFalse(mensagem == null || mensagem.isBlank(),
                    "o cliente precisa entender que nao ha alternativa, nao que o sistema falhou");

            assertFalse(BancoDeTeste.rupturas().buscarPorSessao(sessao).isEmpty(),
                    "sem substituto e o relato mais grave: nao pode se perder");

        } finally {
            String primeiro = decoracao.get(0).get("id").asText();
            chamar("PATCH", "/api/v1/produtos/" + primeiro + "/estoque",
                    "{\"saldoEstoque\":9}");
        }
    }

    // ---------------------------------------------------------------- a forma, vista de cima

    @Test
    @DisplayName("todos os erros tratados pela API tem exatamente a mesma forma")
    void formaUnica() {
        /*
         * O teste que resume o proposito deste arquivo. Se um endpoint novo devolver um erro
         * com outro formato, e aqui que aparece - antes de o frontend descobrir na integracao.
         *
         * O JSON-B omite campo nulo: validationErrors so aparece no erro de validacao, e os
         * outros trazem os cinco campos do contrato.
         */
        UUID sessao = sessaoAberta();
        String caminho404 = "/api/v1/produtos/" + UUID.randomUUID();
        String caminho400 = "/api/v1/sessoes/" + sessao + "/roteiro/itens";
        String caminhoId = "/api/v1/sessoes/nao-e-uuid";

        List<String> cinco = List.of("error", "message", "path", "status", "timestamp");
        List<String> comValidacao = List.of(
                "error", "message", "path", "status", "timestamp", "validationErrors");

        assertEquals(cinco, campos(corpoDe(chamar("GET", caminho404, null))), caminho404);
        assertEquals(cinco, campos(corpoDe(chamar("GET", caminhoId, null))), caminhoId);
        assertEquals(comValidacao, campos(corpoDe(chamar("POST", caminho400, "{}"))), caminho400);
    }

    private static List<String> campos(NoJson erro) {
        List<String> campos = new ArrayList<>();
        erro.fieldNames().forEachRemaining(campos::add);
        Collections.sort(campos);
        return campos;
    }
}
