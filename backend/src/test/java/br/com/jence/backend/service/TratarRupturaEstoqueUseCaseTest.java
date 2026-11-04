package br.com.jence.backend.service;

import br.com.jence.backend.dao.ItemRoteiroDao;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.RegistroRupturaDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.dto.RupturaEstoqueResponse;
import br.com.jence.backend.factory.ConexaoFalsa;
import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.AfinidadeDeProduto;
import br.com.jence.backend.model.FerramentaIA;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.MensagemIA;
import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.RegistroRuptura;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.model.ValorDeAtributo;
import br.com.jence.backend.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.config.Configuracao;
import br.com.jence.backend.ia.GeminiClient;
import br.com.jence.backend.ia.ExecutorDeFerramenta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Roda sem banco: os DAOs sao trocados por versoes em memoria escritas aqui mesmo, e a
 * {@link ConexaoFalsa} entrega a conexao que a {@link br.com.jence.backend.factory.Transacao}
 * abre, confirma e fecha.
 */
class TratarRupturaEstoqueUseCaseTest {

    // ---------------------------------------------------------------- dublês em memoria

    /** Devolve os itens que o teste cadastrou. */
    private static class ItemRoteiroDaoFalso extends ItemRoteiroDao {
        final Map<UUID, ItemRoteiro> itens = new HashMap<>();

        @Override
        public Optional<ItemRoteiro> buscarPorId(UUID id) {
            return Optional.ofNullable(itens.get(id));
        }
    }

    /** Devolve a lista cadastrada para cada item. */
    private static class ListaRoteiroDaoFalso extends ListaRoteiroDao {
        final Map<UUID, ListaRoteiro> porItem = new HashMap<>();

        @Override
        public Optional<ListaRoteiro> buscarPorItem(UUID itemId) {
            return Optional.ofNullable(porItem.get(itemId));
        }
    }

    /** Devolve as sessoes cadastradas e anota cada gravacao. */
    private static class SessaoDaoFalso extends SessaoDao {
        final Map<UUID, Sessao> sessoes = new HashMap<>();
        final List<Sessao> salvas = new ArrayList<>();

        @Override
        public Optional<Sessao> buscarPorId(UUID id) {
            return Optional.ofNullable(sessoes.get(id));
        }

        @Override
        public Sessao salvar(Sessao sessao) {
            salvas.add(sessao);
            return sessao;
        }
    }

    /**
     * A consulta espacial devolve os candidatos informados, mas so quando o produto excluido e o
     * que o teste espera - o mesmo filtro que o {@code eq(...)} do Mockito fazia.
     */
    private static class ProdutoDaoFalso extends ProdutoDao {
        UUID excluidoEsperado;
        List<Produto> candidatos = List.of();

        @Override
        public List<ValorDeAtributo> buscarAtributosDe(UUID produtoId) {
            return List.of();
        }

        @Override
        public List<Produto> buscarDisponiveisProximosDe(PontoMapa referencia, UUID excluido,
                                                         AfinidadeDeProduto afinidade,
                                                         double raio, int limite) {
            return excluido.equals(excluidoEsperado) ? candidatos : List.of();
        }
    }

    /** Anota cada registro gravado. */
    private static class RegistroRupturaDaoFalso extends RegistroRupturaDao {
        final List<RegistroRuptura> salvos = new ArrayList<>();

        @Override
        public RegistroRuptura salvar(RegistroRuptura registro) {
            salvos.add(registro);
            return registro;
        }
    }

    /** Responde o texto combinado, ou falha, e guarda o executor de ferramenta que recebeu. */
    private static class AssistenteFalso extends GeminiClient {
        String resposta;
        AssistenteIAIndisponivelException falha;
        int chamadas;
        ExecutorDeFerramenta executorRecebido;

        AssistenteFalso() {
            super(new Configuracao(Map.of()));
        }

        @Override
        public String conversar(String instrucaoSistema, List<MensagemIA> historico,
                                List<FerramentaIA> ferramentas, ExecutorDeFerramenta executor)
                throws AssistenteIAIndisponivelException {
            chamadas++;
            executorRecebido = executor;
            if (falha != null) {
                throw falha;
            }
            return resposta;
        }
    }

    private ItemRoteiroDaoFalso itemRoteiroDao;
    private ListaRoteiroDaoFalso listaRoteiroDao;
    private SessaoDaoFalso sessaoDao;
    private ProdutoDaoFalso produtoDao;
    private RegistroRupturaDaoFalso registroRupturaDao;
    private AssistenteFalso assistenteIA;
    private TratarRupturaEstoqueUseCase useCase;

    private static final PontoMapa TINTAS =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
    private static final PontoMapa ELETRICA =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Elétrica", 34, 30);

    private UUID sessaoId;
    private UUID itemId;
    private Produto lixaEmFalta;
    private Produto lixaDagua;
    private Produto disjuntor;

    @BeforeEach
    void preparar() {
        ConexaoFalsa.usarNaFabrica();

        itemRoteiroDao = new ItemRoteiroDaoFalso();
        listaRoteiroDao = new ListaRoteiroDaoFalso();
        sessaoDao = new SessaoDaoFalso();
        produtoDao = new ProdutoDaoFalso();
        registroRupturaDao = new RegistroRupturaDaoFalso();
        assistenteIA = new AssistenteFalso();
        useCase = new TratarRupturaEstoqueUseCase(itemRoteiroDao, listaRoteiroDao, sessaoDao,
                produtoDao, registroRupturaDao, assistenteIA);

        sessaoId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        lixaEmFalta = produto("SKU-TIN-003", "Lixa para Parede Grao 120", "3.50", 0, TINTAS);
        lixaDagua = produto("SKU-TIN-004", "Lixa d'Agua Grao 150", "4.20", 40, TINTAS);
        disjuntor = produto("SKU-ELE-003", "Disjuntor Bipolar 25A", "42.90", 15, ELETRICA);
    }

    @AfterEach
    void restaurarFabrica() {
        ConnectionFactory.usarBanco();
    }

    // ---------------------------------------------------------------- montagem do cenario

    private Produto produto(String sku, String nome, String preco, int estoque, PontoMapa ponto) {
        return new Produto(UUID.randomUUID(), sku, nome, new BigDecimal(preco), estoque, ponto);
    }

    /** Roteiro contendo apenas o item cuja prateleira o cliente encontrou vazia. */
    private ItemRoteiro comItemNoRoteiro(Sessao sessao) {
        ItemRoteiro item = ItemRoteiro.reconstituir(itemId, lixaEmFalta, null);
        ListaRoteiro lista = ListaRoteiro.reconstituir(UUID.randomUUID(), sessaoId, List.of(item));

        itemRoteiroDao.itens.put(itemId, item);
        listaRoteiroDao.porItem.put(itemId, lista);
        sessaoDao.sessoes.put(sessaoId, sessao);
        return item;
    }

    private ItemRoteiro comSessaoAtiva() {
        return comItemNoRoteiro(Sessao.iniciar(sessaoId));
    }

    /** A consulta espacial devolve, em ordem de distancia, os candidatos informados. */
    private void comCandidatosProximos(Produto... candidatos) {
        produtoDao.excluidoEsperado = lixaEmFalta.getId();
        produtoDao.candidatos = List.of(candidatos);
    }

    private void assistenteResponde(String resposta) {
        assistenteIA.resposta = resposta;
    }

    /** O unico registro gravado; falha se houver nenhum ou mais de um. */
    private RegistroRuptura registroSalvo() {
        assertEquals(1, registroRupturaDao.salvos.size(), "a ruptura deveria ser registrada uma vez");
        return registroRupturaDao.salvos.getFirst();
    }

    // ---------------------------------------------------------------- caminho feliz

    @Test
    @DisplayName("o assistente elege um candidato e a sugestao sai com o produto do nosso banco")
    void assistenteElegeSubstituto() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("SKU-TIN-004 | A lixa d'agua grao 150 da o mesmo acabamento e esta no "
                + "mesmo corredor.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertEquals(lixaEmFalta.getId(), resposta.produtoOriginalId());
        assertEquals(lixaDagua.getId(), resposta.produtoSugeridoId());
        assertEquals("SKU-TIN-004", resposta.produtoSugerido().sku());
        assertEquals(0, resposta.produtoSugerido().preco().compareTo(new BigDecimal("4.20")));
        assertTrue(resposta.justificativa().startsWith("A lixa d'agua"));
        assertEquals(OrigemSugestao.ASSISTENTE_IA, resposta.origemSugestao());
    }

    @Test
    @DisplayName("o assistente so recebe os candidatos da pre-filtragem espacial")
    void ferramentaDevolveApenasOsCandidatosFiltrados() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("SKU-TIN-004 | serve para o mesmo acabamento");

        useCase.executar(itemId);

        assertEquals(1, assistenteIA.chamadas);
        ExecutorDeFerramenta executor = assistenteIA.executorRecebido;
        assertNotNull(executor);

        Object candidatos = executor.executar("buscar_substitutos_proximos", Map.of()).get("candidatos");

        String mensagem = "a ferramenta nao pode devolver nada alem do que a consulta espacial trouxe";
        List<?> lista = assertInstanceOf(List.class, candidatos);
        assertEquals(2, lista.size(), mensagem);
        for (Object c : lista) {
            assertTrue(List.of("SKU-TIN-004", "SKU-ELE-003").contains(((Map<?, ?>) c).get("sku")), mensagem);
        }

        // A distancia informada ao modelo e a real: mesma secao = 0, Tintas -> Eletrica = 20.
        assertEquals(0L, ((Map<?, ?>) ((List<?>) candidatos).get(0)).get("distancia"));
        assertEquals(20L, ((Map<?, ?>) ((List<?>) candidatos).get(1)).get("distancia"));
    }

    @Test
    @DisplayName("a ruptura fica registrada com a origem da sugestao")
    void registraARuptura() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua);
        assistenteResponde("SKU-TIN-004 | mesmo acabamento, mesmo corredor");

        useCase.executar(itemId);

        RegistroRuptura registro = registroSalvo();
        assertEquals(sessaoId, registro.getSessaoId());
        assertEquals(itemId, registro.getItemRoteiroId());
        assertEquals(lixaEmFalta.getId(), registro.getProdutoFaltanteId());
        assertEquals(lixaDagua.getId(), registro.getProdutoSugeridoId());
        assertEquals(OrigemSugestao.ASSISTENTE_IA, registro.getOrigem());
        assertTrue(registro.temSugestao());
    }

    @Test
    @DisplayName("a sessao e renovada: o cliente esta em plena caminhada pela loja")
    void renovaASessao() throws Exception {
        Sessao sessao = Sessao.reconstituir(sessaoId, StatusSessao.ACTIVE,
                LocalDateTime.now().minusMinutes(25), LocalDateTime.now().plusMinutes(5), null, null);
        comItemNoRoteiro(sessao);
        comCandidatosProximos(lixaDagua);
        assistenteResponde("SKU-TIN-004 | mesmo acabamento");

        useCase.executar(itemId);

        assertTrue(sessao.getExpiracaoTtl().isAfter(LocalDateTime.now().plusMinutes(25)));
        assertEquals(1, sessaoDao.salvas.stream().filter(s -> s == sessao).count(),
                "a sessao renovada deveria ser gravada uma vez");
    }

    // ---------------------------------------------------------------- grounding

    @Test
    @DisplayName("SKU fora da lista de candidatos e descartado e cai no mais proximo")
    void skuInventadoNaoViraSugestao() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        // Produto que existe no mundo, mas nao estava entre os candidatos que oferecemos.
        assistenteResponde("SKU-MAT-999 | Leve a massa corrida premium, e o que voce precisa.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertEquals(lixaDagua.getId(), resposta.produtoSugeridoId(),
                "nenhum produto pode sair daqui sem ter vindo da nossa pre-filtragem");
        assertEquals(OrigemSugestao.PROXIMIDADE, resposta.origemSugestao());
        assertFalse(resposta.justificativa().contains("massa corrida"),
                "a justificativa inventada pelo modelo nao pode chegar ao cliente");
    }

    @Test
    @DisplayName("resposta fora do formato combinado cai no mais proximo")
    void respostaIlegivelCaiNoFallback() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("Acho que voce deveria procurar um vendedor.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertEquals(lixaDagua.getId(), resposta.produtoSugeridoId());
        assertEquals(OrigemSugestao.PROXIMIDADE, resposta.origemSugestao());
    }

    @Test
    @DisplayName("negrito e crase em volta do SKU nao invalidam a escolha")
    void toleraMarkdownNoCodigo() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("**SKU-TIN-004** | Mesmo acabamento, poucos passos a frente.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertEquals(lixaDagua.getId(), resposta.produtoSugeridoId());
        assertEquals(OrigemSugestao.ASSISTENTE_IA, resposta.origemSugestao());
    }

    // ---------------------------------------------------------------- degradacao

    @Test
    @DisplayName("assistente fora do ar nao deixa o cliente sem resposta")
    void assistenteIndisponivelCaiNoMaisProximo() throws Exception {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteIA.falha = new AssistenteIAIndisponivelException("cota esgotada");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertEquals(lixaDagua.getId(), resposta.produtoSugeridoId());
        assertEquals(OrigemSugestao.PROXIMIDADE, resposta.origemSugestao());
        assertTrue(resposta.justificativa().contains("mais próximo"),
                "a mensagem precisa ser honesta sobre nao ter havido analise");
        assertEquals(OrigemSugestao.PROXIMIDADE, registroSalvo().getOrigem());
    }

    // ---------------------------------------------------------------- sem substituto

    @Test
    @DisplayName("sem candidato proximo, registra a ruptura e devolve 422")
    void semCandidatosProximos() {
        ItemRoteiro item = comSessaoAtiva();
        comCandidatosProximos();

        SubstitutoIndisponivelException erro =
                assertThrows(SubstitutoIndisponivelException.class, () -> useCase.executar(itemId));
        assertTrue(erro.getMessage().contains("Lixa para Parede Grao 120"));

        RegistroRuptura registro = registroSalvo();
        assertFalse(registro.temSugestao());
        assertEquals(OrigemSugestao.NENHUMA, registro.getOrigem());
        assertEquals(item.getProduto().getId(), registro.getProdutoFaltanteId());
        assertEquals(0, assistenteIA.chamadas);
    }

    @Test
    @DisplayName("assistente que recusa todos os candidatos nao cai no fallback")
    void assistenteRecusaTodosOsCandidatos() {
        comSessaoAtiva();
        comCandidatosProximos(disjuntor);
        assistenteResponde("NENHUM | Nao ha por perto nenhum item de acabamento equivalente a lixa.");

        assertThrows(SubstitutoIndisponivelException.class, () -> useCase.executar(itemId),
                "sugerir algo inadequado e pior do que nao sugerir nada");

        RegistroRuptura registro = registroSalvo();
        assertFalse(registro.temSugestao());
        assertTrue(registro.getJustificativa().contains("item de acabamento equivalente"));
    }

    @Test
    @DisplayName("produto que o cliente ja vai levar nao entra como candidato")
    void ignoraCandidatoQueJaEstaNoRoteiro() throws Exception {
        ItemRoteiro item = ItemRoteiro.reconstituir(itemId, lixaEmFalta, null);
        ItemRoteiro jaNoCarrinho = ItemRoteiro.reconstituir(UUID.randomUUID(), lixaDagua, null);
        ListaRoteiro lista = ListaRoteiro.reconstituir(
                UUID.randomUUID(), sessaoId, List.of(item, jaNoCarrinho));

        itemRoteiroDao.itens.put(itemId, item);
        listaRoteiroDao.porItem.put(itemId, lista);
        sessaoDao.sessoes.put(sessaoId, Sessao.iniciar(sessaoId));
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("SKU-ELE-003 | Nao ha lixa equivalente; leve o disjuntor.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertEquals(disjuntor.getId(), resposta.produtoSugeridoId(),
                "a lixa d'agua ja esta no carrinho e nao pode ser oferecida de novo");
    }

    // ---------------------------------------------------------------- pre-condicoes

    @Test
    @DisplayName("item inexistente devolve 404")
    void itemInexistente() {
        // Nenhum item cadastrado no DAO falso: a busca volta vazia.

        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.executar(itemId));

        assertTrue(registroRupturaDao.salvos.isEmpty());
    }

    @Test
    @DisplayName("sessao encerrada devolve 409 e nao consulta a IA")
    void sessaoInativa() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        comItemNoRoteiro(encerrada);

        OperacaoNaoPermitidaException erro =
                assertThrows(OperacaoNaoPermitidaException.class, () -> useCase.executar(itemId));
        assertTrue(erro.getMessage().contains("COMPLETED"));

        assertEquals(0, assistenteIA.chamadas);
        assertTrue(registroRupturaDao.salvos.isEmpty());
    }
}
