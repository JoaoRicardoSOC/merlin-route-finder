package br.com.jence.backend;

import br.com.jence.backend.dao.RegistroRupturaDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.dto.ListaRoteiroResponse;
import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.ConcluirRotaUseCase;
import br.com.jence.backend.service.ConsultarSessaoUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.service.SubstituirItemDoRoteiroUseCase;
import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.RegistroRuptura;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * A troca do produto que faltou pelo substituto, que e o que fecha o ciclo da ruptura.
 * <p>
 * A promessa do recurso e converter prateleira vazia em venda. Se aceitar a sugestao der
 * trabalho - duas acoes em pe no corredor -, a conversao nao acontece e o recurso vira
 * decoracao.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class SubstituicaoDeItemIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired MarcarItemColetadoUseCase marcarColetado;
    @Autowired SubstituirItemDoRoteiroUseCase substituir;
    @Autowired ConsultarSessaoUseCase consultar;
    @Autowired ConcluirRotaUseCase concluir;
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();
    private final RegistroRupturaDao registroRupturaDao = BancoDeTeste.rupturas();


    /** A lixa grao 120 e o item que some da prateleira; a lixa d'agua e o substituto. */
    private static final String EM_FALTA = "SKU-TIN-003";
    private static final String SUBSTITUTO = "SKU-TIN-004";

    private final List<UUID> criadas = new ArrayList<>();

    @AfterEach
    void limpar() throws Exception {
        for (UUID id : criadas) {
            /*
             * O registro de ruptura aponta para a sessao por chave estrangeira, entao apagar a
             * sessao antes dele e recusado com ORA-02292. E o mesmo motivo pelo qual a limpeza
             * de outros testes de integracao falha em silencio quando uma ruptura acontece
             * neles - ver O-20. Por isso o BancoDeTeste.apagarSessao apaga os relatos antes.
             */
            BancoDeTeste.apagarSessao(id);
        }
        criadas.clear();
    }

    private UUID novaSessao() throws Exception {
        SessaoResponse sessao = inicializar.executar("ENT-01");
        criadas.add(sessao.id());
        return sessao.id();
    }

    private UUID idDe(String sku) throws Exception {
        return produtoDao.buscarPorSku(sku).orElseThrow().getId();
    }

    private UUID adicionar(UUID sessaoId, String sku) throws Exception {
        return adicionar.executar(sessaoId, idDe(sku)).getId();
    }

    private List<String> skusDaLista(ListaRoteiroResponse lista) {
        return lista.itens().stream().map(item -> item.getProduto().getSku()).sorted().toList();
    }

    // ---------------------------------------------------------------- a troca

    @Test
    @DisplayName("numa chamada so, o substituto entra e o item em falta sai")
    void trocaNumaChamada() throws Exception {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        adicionar(sessaoId, "SKU-ENC-001");

        ListaRoteiroResponse depois = substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertEquals(List.of("SKU-ENC-001", SUBSTITUTO), skusDaLista(depois));
        assertEquals(2, depois.quantidadeItens(), "trocar nao pode mudar o tamanho da lista");
    }

    @Test
    @DisplayName("o substituto entra NAO coletado, mesmo trocando um item ja coletado")
    void substitutoEntraNaoColetado() throws Exception {
        /*
         * O detalhe que decide se o cliente sai da loja com o produto. O substituto pode estar
         * alguns metros adiante, e herdar o estado do item que saiu o marcaria como pego -
         * fazendo o cliente ir embora sem ele, e mentindo sobre onde ele esta.
         */
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        marcarColetado.executar(lixaEmFalta);

        ListaRoteiroResponse depois = substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertEquals(1, depois.itens().size());
        ItemRoteiroDetalhadoResponse item = depois.itens().get(0);
        assertEquals(SUBSTITUTO, item.getProduto().getSku());
        assertFalse(item.isColetado());
    }

    @Test
    @DisplayName("o substituto que ja estava na lista nao vira duplicata")
    void semDuplicata() throws Exception {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        adicionar(sessaoId, SUBSTITUTO);

        ListaRoteiroResponse depois = substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertEquals(List.of(SUBSTITUTO), skusDaLista(depois),
                "o cliente ja tinha a lixa d'agua na lista; ela nao pode aparecer duas vezes");
    }

    @Test
    @DisplayName("a posicao do cliente nao pula para a prateleira do substituto")
    void posicaoNaoPula() throws Exception {
        // Ele ainda nao foi buscar o substituto: a posicao continua sendo a ultima evidencia
        // real, e nao um palpite sobre onde ele vai estar.
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertEquals("ENT01", consultar.executar(sessaoId).posicaoAtual().getCodigoCurto());
    }

    // ---------------------------------------------------------------- o que a troca nao apaga

    @Test
    @DisplayName("a ruptura continua registrada depois da troca")
    void rupturaPermanece() throws Exception {
        /*
         * O registro e evidencia do que aconteceu na gondola, e vale tenha o cliente aceitado
         * a troca ou nao. Apaga-lo faria a loja perder exatamente o dado que o recurso existe
         * para produzir - e comparar quantas rupturas viraram troca e o que diz se as
         * sugestoes estao boas.
         *
         * O registro e plantado direto pelo repositorio, e nao pelo caso de uso da ruptura,
         * para o teste nao depender do assistente de IA: a cota gratuita torna a suite
         * instavel (O-01), e o que se verifica aqui e a troca, nao a sugestao.
         */
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        registroRupturaDao.salvar(RegistroRuptura.comSugestao(
                sessaoId, lixaEmFalta, idDe(EM_FALTA), idDe(SUBSTITUTO),
                "A lixa d'agua grao 150 da o mesmo acabamento.", OrigemSugestao.ASSISTENTE_IA));

        substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        List<RegistroRuptura> registros = registroRupturaDao.buscarPorSessao(sessaoId);
        assertEquals(1, registros.size(), "trocar o item nao pode apagar o relato da prateleira vazia");
        RegistroRuptura registro = registros.get(0);
        assertEquals(idDe(EM_FALTA), registro.getProdutoFaltanteId());
        assertEquals(lixaEmFalta, registro.getItemRoteiroId(),
                "o registro aponta para um item que ja nao existe, e tudo bem: "
                        + "ele descreve o que aconteceu, nao o estado de agora");
    }

    // ---------------------------------------------------------------- recusas

    @Test
    @DisplayName("trocar um produto por ele mesmo e recusado, e nao apaga o item")
    void naoSubstituiPorSiMesmo() throws Exception {
        /*
         * Sem a guarda isto apagaria o item em silencio: o adicionar devolveria o item
         * existente e o remover o apagaria em seguida. O cliente ficaria sem o produto sem ter
         * pedido isso.
         */
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> substituir.executar(lixaEmFalta, idDe(EM_FALTA)));

        assertEquals(1, BancoDeTeste.listas().buscarPorSessao(sessaoId).orElseThrow().getItens().size());
    }

    @Test
    @DisplayName("produto substituto inexistente devolve 404 e a lista fica intacta")
    void substitutoInexistente() throws Exception {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        assertThrows(RecursoNaoEncontradoException.class,
                () -> substituir.executar(lixaEmFalta, UUID.randomUUID()));

        assertEquals(1, BancoDeTeste.listas().buscarPorSessao(sessaoId).orElseThrow().getItens().size());
    }

    @Test
    @DisplayName("sessao encerrada recusa a troca")
    void sessaoEncerrada() throws Exception {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        concluir.executar(sessaoId);

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> substituir.executar(lixaEmFalta, idDe(SUBSTITUTO)));
    }

    @Test
    @DisplayName("item inexistente devolve 404")
    void itemInexistente() throws Exception {
        assertThrows(RecursoNaoEncontradoException.class,
                () -> substituir.executar(UUID.randomUUID(), idDe(SUBSTITUTO)));
    }
}
