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
 * A jornada do cliente percorrida por HTTP, da placa na parede ao encerramento.
 * <p>
 * <b>E o nivel que importa para a integracao com o frontend.</b> Os testes de
 * {@code @WebMvcTest} exercitam um controller com casos de uso simulados; aqui sobe a
 * aplicacao inteira e cada chamada passa por serializacao, status HTTP, tratamento de erro e
 * banco real - exatamente o que o celular do cliente vai encontrar.
 * <p>
 * <b>Reescrito em 25/08/2026 para o escopo revisado.</b> A versao anterior descrevia a jornada
 * antiga e nao tocava em metade do produto: entrada por placa, mapa, filtros, facetas,
 * recentrar, desmarcar e aceitar substituto ficavam de fora. Um teste de vitrine que descreve
 * um produto que nao existe mais e pior que nenhum, porque passa e da confianca.
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

    /**
     * A lixa grao 120: entra no roteiro com estoque e, na prateleira, o cliente nao a acha.
     * E o gatilho da ruptura, e o estoque positivo e condicao para ela poder entrar (D-72).
     */
    private static final String EM_FALTA = "SKU-TIN-003";

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

    private int status(HttpMethod metodo, String caminho, String corpo) {
        return chamar(metodo, caminho, corpo).getStatusCode().value();
    }

    private void passo(String descricao) {
        System.out.println(">>> " + descricao);
    }

    // ---------------------------------------------------------------- montagem

    private UUID entrarPelaPlaca(String codigo) {
        String corpo = codigo == null ? null : "{\"codigoPonto\":\"" + codigo + "\"}";
        ResponseEntity<String> r = chamar(HttpMethod.POST, "/sessoes", corpo);
        assertThat(r.getStatusCode().value()).isEqualTo(201);

        UUID id = UUID.fromString(corpoDe(r).get("id").asText());
        sessoesCriadas.add(id);
        return id;
    }

    private String idDoProduto(String sku) {
        for (JsonNode produto : get("/produtos?size=100&query=" + sku.substring(8)).get("content")) {
            if (sku.equals(produto.get("sku").asText())) {
                return produto.get("id").asText();
            }
        }
        // O SKU nao serve de termo de busca para todo produto; cai para a varredura por secao.
        for (JsonNode secao : get("/produtos/secoes")) {
            for (JsonNode produto : get("/produtos?size=100&secao="
                    + secao.get("nome").asText().replace(" ", "%20")).get("content")) {
                if (sku.equals(produto.get("sku").asText())) {
                    return produto.get("id").asText();
                }
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

    private String corredorAtual(UUID sessaoId) {
        JsonNode posicao = get("/sessoes/" + sessaoId).get("posicaoAtual");
        return posicao.isNull() ? null : posicao.get("corredor").asText();
    }

    // ---------------------------------------------------------------- a jornada inteira

    @Test
    @DisplayName("da placa na parede ao encerramento, passando por tudo que o cliente faz")
    void jornadaDoClienteNaLoja() {

        // ---- 1. entra escaneando a placa da entrada
        UUID sessao = entrarPelaPlaca("ENT-01");
        JsonNode aberta = get("/sessoes/" + sessao);

        assertThat(aberta.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(aberta.get("posicaoAtual").get("codigoCurto").asText()).isEqualTo("ENT01");
        passo("entrou pela placa " + aberta.get("posicaoAtual").get("corredor").asText());

        // ---- 2. abre o mapa, que nao depende da sessao
        JsonNode mapa = get("/mapa");

        assertThat(mapa.get("largura").asInt()).isEqualTo(100);
        assertThat(mapa.get("blocos")).isNotEmpty();
        assertThat(mapa.get("pontos")).isNotEmpty();
        passo("mapa com " + mapa.get("blocos").size() + " corredores e "
                + mapa.get("pontos").size() + " pontos de servico");

        // ---- 3. navega pelo menu de secoes
        JsonNode secoes = get("/produtos/secoes");
        assertThat(secoes).isNotEmpty();
        assertThat(secoes.get(0).get("quantidadeProdutos").asInt()).isPositive();

        JsonNode emTintas = get("/produtos?secao=Tintas&size=50");
        assertThat(emTintas.get("content")).isNotEmpty();
        passo("Tintas tem " + emTintas.get("totalElements").asInt() + " produtos");

        // ---- 4. usa uma faceta que a propria resposta ofereceu
        JsonNode facetaMarca = null;
        for (JsonNode faceta : emTintas.get("facetas")) {
            if ("MARCA".equals(faceta.get("atributo").asText())) {
                facetaMarca = faceta;
            }
        }
        assertThat(facetaMarca)
                .as("a tela de catalogo precisa de filtros para oferecer")
                .isNotNull();

        String marca = facetaMarca.get("valores").get(0).get("valor").asText();
        long esperados = facetaMarca.get("valores").get(0).get("quantidade").asLong();

        JsonNode filtrado = get("/produtos?secao=Tintas&size=50&atributo=MARCA:" + marca);

        assertThat(filtrado.get("totalElements").asLong())
                .as("a contagem da faceta precisa bater com o filtro que ela oferece")
                .isEqualTo(esperados);
        passo("filtrou Tintas por marca " + marca + " -> " + esperados + " produto(s)");

        // ---- 5. busca errando a digitacao
        JsonNode busca = get("/produtos?query=tnta&size=20");
        assertThat(busca.get("content")).isNotEmpty();
        passo("busca por 'tnta' -> " + busca.get("content").get(0).get("nome").asText());

        // ---- 6. abre o detalhe e ve as especificacoes
        String tinta = busca.get("content").get(0).get("id").asText();
        JsonNode detalhe = get("/produtos/" + tinta);

        assertThat(detalhe.get("descricao").asText()).isNotBlank();
        assertThat(detalhe.get("pontoMapa").get("corredor").asText()).isNotBlank();
        assertThat(detalhe.get("atributos"))
                .as("a tela de detalhe mostra a tabela de especificacoes")
                .isNotEmpty();
        passo("detalhe de " + detalhe.get("nome").asText() + " com "
                + detalhe.get("atributos").size() + " caracteristicas");

        // ---- 7. monta a lista
        String cano = idDoProduto("SKU-ENC-001");
        String lixaZerada = idDoProduto(EM_FALTA);

        adicionar(sessao, tinta);
        String itemCano = adicionar(sessao, cano).get("id").asText();
        String itemLixa = adicionar(sessao, lixaZerada).get("id").asText();

        JsonNode lista = get("/sessoes/" + sessao + "/roteiro");
        assertThat(lista.get("quantidadeItens").asInt()).isEqualTo(3);
        passo("lista montada com 3 itens");

        // ---- 8. caminha e coleta: a posicao acompanha
        assertThat(status(HttpMethod.PATCH, "/roteiro/itens/" + itemCano + "/coletar", null))
                .isEqualTo(200);

        assertThat(corredorAtual(sessao))
                .as("depois de pegar o cano, o cliente esta em Encanamento")
                .isEqualTo("Encanamento");
        passo("coletou o cano -> posicao em Encanamento");

        // ---- 9. tocou por engano: desmarca e a posicao volta
        assertThat(status(HttpMethod.PATCH, "/roteiro/itens/" + itemCano + "/desmarcar", null))
                .isEqualTo(200);

        assertThat(corredorAtual(sessao))
                .as("desfeita a coleta, ele volta a estar na placa de entrada")
                .isEqualTo("Entrada da loja");
        passo("desmarcou -> posicao de volta na entrada");

        // recoloca, porque ele de fato pegou o cano
        chamar(HttpMethod.PATCH, "/roteiro/itens/" + itemCano + "/coletar", null);

        // ---- 10. chega na prateleira da lixa e ela esta vazia
        ResponseEntity<String> respostaRuptura =
                chamar(HttpMethod.POST, "/roteiro/itens/" + itemLixa + "/ruptura", null);

        assertThat(respostaRuptura.getStatusCode().value())
                .as("a massa garante um substituto plausivel para a lixa")
                .isEqualTo(200);

        JsonNode ruptura = corpoDe(respostaRuptura);
        JsonNode sugerido = ruptura.get("produtoSugerido");

        assertThat(sugerido.get("saldoEstoque").asInt())
                .as("nao adianta sugerir algo que tambem acabou")
                .isPositive();
        assertThat(ruptura.get("origemSugestao").asText())
                .isIn("ASSISTENTE_IA", "PROXIMIDADE");
        passo("prateleira vazia -> " + sugerido.get("nome").asText()
                + " (" + ruptura.get("origemSugestao").asText() + ")");

        // ---- 11. aceita o substituto: uma acao so
        ResponseEntity<String> troca = chamar(HttpMethod.POST,
                "/roteiro/itens/" + itemLixa + "/substituir",
                "{\"produtoSubstitutoId\":\"" + sugerido.get("id").asText() + "\"}");

        assertThat(troca.getStatusCode().value()).isEqualTo(200);
        JsonNode depoisDaTroca = corpoDe(troca);

        assertThat(depoisDaTroca.get("quantidadeItens").asInt())
                .as("trocar nao muda o tamanho da lista")
                .isEqualTo(3);

        List<String> skus = new ArrayList<>();
        depoisDaTroca.get("itens").forEach(item -> skus.add(item.get("produto").get("sku").asText()));

        assertThat(skus).doesNotContain(EM_FALTA);
        assertThat(skus).contains(sugerido.get("sku").asText());
        passo("substituto aceito numa chamada; a lixa em falta saiu da lista");

        // ---- 12. se perde e le outra placa
        assertThat(status(HttpMethod.PUT, "/sessoes/" + sessao + "/posicao",
                "{\"codigoPonto\":\"CEN-03\"}")).isEqualTo(200);

        assertThat(corredorAtual(sessao)).isEqualTo("Cruzamento central");
        assertThat(get("/sessoes/" + sessao + "/roteiro").get("quantidadeItens").asInt())
                .as("recentrar nao pode mexer na lista")
                .isEqualTo(3);
        passo("recentrou no cruzamento central, com a lista intacta");

        // ---- 13. encerra
        JsonNode encerrada = corpoDe(chamar(HttpMethod.POST, "/sessoes/" + sessao + "/concluir", null));
        assertThat(encerrada.get("status").asText()).isEqualTo("COMPLETED");
        passo("jornada concluida");

        // ---- 14. depois de encerrar: le, mas nao escreve
        assertThat(status(HttpMethod.GET, "/sessoes/" + sessao + "/roteiro", null))
                .as("a lista continua legivel depois de encerrada (D-41)")
                .isEqualTo(200);

        assertThat(status(HttpMethod.POST, "/sessoes/" + sessao + "/roteiro/itens",
                "{\"produtoId\":\"" + tinta + "\"}"))
                .as("escrever numa sessao encerrada e 409, e o frontend traduz isso")
                .isEqualTo(409);
        passo("sessao encerrada: leitura sim, escrita 409");
    }

    // ---------------------------------------------------------------- entradas que dao errado

    @Test
    @DisplayName("placa desconhecida nao barra a entrada: a sessao nasce sem posicao e funciona")
    void placaDesconhecidaNaoBarraAEntrada() {
        /*
         * A assimetria da D-57 vista de fora: aqui nao ha erro HTTP nenhum, e a unica pista de
         * que algo deu errado e posicaoAtual vir nula. E por isso que a tela precisa olhar
         * esse campo - se ignorar, o cliente digita errado e nao entende por que o mapa nao o
         * localiza.
         */
        UUID sessao = entrarPelaPlaca("ZZZ-99");

        assertThat(get("/sessoes/" + sessao).get("posicaoAtual").isNull()).isTrue();

        String produto = idDoProduto("SKU-ENC-001");
        assertThat(status(HttpMethod.POST, "/sessoes/" + sessao + "/roteiro/itens",
                "{\"produtoId\":\"" + produto + "\"}"))
                .as("sem posicao, mas com o sistema inteiro disponivel")
                .isEqualTo(201);

        passo("placa desconhecida: sessao viva, posicao nula");
    }

    @Test
    @DisplayName("recentrar com placa desconhecida e recusado, e a posicao anterior fica de pe")
    void recentrarComPlacaDesconhecida() {
        // O outro lado da mesma assimetria: aqui o cliente ja tem sessao, e avisar e acionavel.
        UUID sessao = entrarPelaPlaca("ENT-01");

        assertThat(status(HttpMethod.PUT, "/sessoes/" + sessao + "/posicao",
                "{\"codigoPonto\":\"ZZZ-99\"}")).isEqualTo(404);

        assertThat(corredorAtual(sessao))
                .as("errar a digitacao nao pode apagar o que o sistema ja sabia")
                .isEqualTo("Entrada da loja");
    }

    @Test
    @DisplayName("o codigo da placa e aceito em qualquer grafia")
    void codigoEmQualquerGrafia() {
        // O plano B: quem nao consegue escanear digita o que le na placa, do jeito que le.
        // O codigo vai no CORPO da requisicao, entao nao ha nada a escapar - foi o erro da
        // primeira versao deste teste, que codificava o espaco como se fosse URL.
        for (String grafia : List.of("TIN-02", "tin-02", "TIN02", "tin 02")) {
            UUID sessao = entrarPelaPlaca(grafia);
            assertThat(get("/sessoes/" + sessao).get("posicaoAtual").get("codigoCurto").asText())
                    .as("grafia %s", grafia)
                    .isEqualTo("TIN02");
        }
    }

    // ---------------------------------------------------------------- retomada e demonstracao

    @Test
    @DisplayName("celular que perdeu a aba recupera tudo so com o id da sessao")
    void celularSeRecuperaComOIdDaSessao() {
        /*
         * O unico estado que o aparelho guarda e o sessaoId, no localStorage. Tudo o mais -
         * lista, itens coletados, posicao - volta do banco. E o que impede uma aba fechada sem
         * querer de virar "comece tudo de novo" no meio da loja.
         */
        UUID sessao = entrarPelaPlaca("ENT-01");
        String cano = idDoProduto("SKU-ENC-001");
        String item = adicionar(sessao, cano).get("id").asText();
        chamar(HttpMethod.PATCH, "/roteiro/itens/" + item + "/coletar", null);

        // ---- daqui para baixo, o app so tem o id da sessao
        JsonNode estado = get("/sessoes/" + sessao);
        JsonNode lista = get("/sessoes/" + sessao + "/roteiro");

        assertThat(estado.get("status").asText()).isEqualTo("ACTIVE");
        assertThat(estado.get("posicaoAtual").get("corredor").asText()).isEqualTo("Encanamento");
        assertThat(lista.get("quantidadeItens").asInt()).isEqualTo(1);
        assertThat(lista.get("itens").get(0).get("coletado").asBoolean()).isTrue();

        passo("recuperacao pelo id da sessao: posicao e coleta preservadas");
    }

    @Test
    @DisplayName("a simulacao de estoque cria e desfaz o cenario de ruptura sob demanda")
    void simulacaoDeEstoqueCriaEDesfazOCenario() {
        String trena = idDoProduto("SKU-FER-002");
        int saldoOriginal = get("/produtos/" + trena).get("saldoEstoque").asInt();

        try {
            ResponseEntity<String> zerado = chamar(HttpMethod.PATCH,
                    "/produtos/" + trena + "/estoque", "{\"saldoEstoque\":0}");
            assertThat(zerado.getStatusCode().value()).isEqualTo(200);
            assertThat(corpoDe(zerado).get("saldoEstoque").asInt()).isZero();

            UUID sessao = entrarPelaPlaca("ENT-01");
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
