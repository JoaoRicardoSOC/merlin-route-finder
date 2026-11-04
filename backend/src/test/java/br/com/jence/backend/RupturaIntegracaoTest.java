package br.com.jence.backend;

import br.com.jence.backend.dao.RegistroRupturaDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.dto.RupturaEstoqueResponse;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.RegistroRuptura;
import br.com.jence.backend.exception.SubstitutoIndisponivelException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O cenario de ruptura de ponta a ponta, contra o Oracle e o Gemini reais: o cliente adiciona
 * a lixa grao 120 ao roteiro, chega a prateleira e nao encontra o produto la. Ele reporta a
 * gondola vazia, e o assistente elege o substituto.
 * <p>
 * E o card inteiro em um teste - e o mesmo roteiro que sera mostrado na banca.
 */
@Tag("integracao")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class RupturaIntegracaoTest {

    private static final String LIXA_EM_FALTA = "SKU-TIN-003";
    private static final String LIXA_SUBSTITUTA = "SKU-TIN-004";

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired TratarRupturaEstoqueUseCase tratarRuptura;
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();
    private final RegistroRupturaDao registroRupturaDao = BancoDeTeste.rupturas();


    private UUID sessaoId;

    /* O tier gratuito limita requisicoes por minuto: um respiro evita medir cota em vez de comportamento. */
    @BeforeEach
    void criarSessao() throws Exception {
        Thread.sleep(5_000);
        sessaoId = inicializar.executar(null).id();
    }

    @AfterEach
    void limpar() throws Exception {
        BancoDeTeste.apagarSessao(sessaoId);
    }

    private ItemRoteiroDetalhadoResponse adicionarAoRoteiro(String sku) throws Exception {
        Produto produto = produtoDao.buscarPorSku(sku).orElseThrow();
        return adicionar.executar(sessaoId, produto.getId());
    }

    @Test
    void elegeOSubstitutoSemanticoEntreOsVizinhosDisponiveis() throws Exception {
        ItemRoteiroDetalhadoResponse item = adicionarAoRoteiro(LIXA_EM_FALTA);

        RupturaEstoqueResponse resposta = tratarRuptura.executar(item.id());

        System.out.println(">>> em falta:   " + item.produto().nome());
        System.out.println(">>> sugerido:   " + resposta.produtoSugerido().nome()
                + " (" + resposta.produtoSugerido().sku() + ")");
        System.out.println(">>> corredor:   " + resposta.produtoSugerido().pontoMapa().corredor());
        System.out.println(">>> origem:     " + resposta.origemSugestao());
        System.out.println(">>> justifica.: " + resposta.justificativa());

        assertEquals(OrigemSugestao.ASSISTENTE_IA, resposta.origemSugestao(),
                "se cair em PROXIMIDADE, o assistente nao respondeu - verifique a cota da chave");

        assertEquals(LIXA_SUBSTITUTA, resposta.produtoSugerido().sku(),
                "no mesmo raio ha disjuntor, cabo e parafuso; so a lixa d'agua cumpre a funcao");

        assertTrue(resposta.produtoSugerido().saldoEstoque() > 0);
        assertFalse(resposta.justificativa() == null || resposta.justificativa().isBlank());
    }

    @Test
    void registraARupturaParaAOperacaoDaLoja() throws Exception {
        ItemRoteiroDetalhadoResponse item = adicionarAoRoteiro(LIXA_EM_FALTA);

        tratarRuptura.executar(item.id());

        List<RegistroRuptura> registros = registroRupturaDao.buscarPorSessao(sessaoId);

        assertEquals(1, registros.size());
        assertEquals(item.id(), registros.getFirst().getItemRoteiroId());
        assertTrue(registros.getFirst().temSugestao());
        assertFalse(registros.getFirst().getJustificativa() == null
                || registros.getFirst().getJustificativa().isBlank());

        System.out.println(">>> registro persistido: " + registros.getFirst().getOrigem()
                + " em " + registros.getFirst().getRegistradoEm());
    }

    @Test
    void produtoIsoladoNaLojaNaoRendeSugestaoENemDeixaDeSerRegistrado() throws Exception {
        // O espelho de Decoracao esta em (88,55), a mais de 25 unidades de qualquer outra
        // secao: nao ha vizinho dentro do raio, e a ruptura precisa ser registrada mesmo assim.
        ItemRoteiroDetalhadoResponse item = adicionarAoRoteiro("SKU-DEC-001");

        assertThrows(SubstitutoIndisponivelException.class, () -> tratarRuptura.executar(item.id()));

        List<RegistroRuptura> registros = registroRupturaDao.buscarPorSessao(sessaoId);

        assertEquals(1, registros.size());
        assertFalse(registros.getFirst().temSugestao());
        assertEquals(OrigemSugestao.NENHUMA, registros.getFirst().getOrigem());
    }
}
