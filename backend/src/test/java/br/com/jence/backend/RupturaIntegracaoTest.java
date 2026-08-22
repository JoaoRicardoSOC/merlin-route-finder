package br.com.jence.backend;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.dto.RupturaEstoqueResponse;
import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.domain.entity.OrigemSugestao;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.RegistroRuptura;
import br.com.jence.backend.domain.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.RegistroRupturaRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.RegistroRupturaJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * O cenario de ruptura de ponta a ponta, contra o Oracle e o Gemini reais: o cliente adiciona
 * a lixa grao 120 (zerada na massa de demonstracao) ao roteiro, chega a prateleira, encontra
 * a gondola vazia e o assistente elege o substituto.
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
    @Autowired ProdutoRepository produtoRepository;
    @Autowired RegistroRupturaRepository registroRupturaRepository;

    @Autowired RegistroRupturaJpaRepository registroJpa;
    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;

    private UUID sessaoId;

    /* O tier gratuito limita requisicoes por minuto: um respiro evita medir cota em vez de comportamento. */
    @BeforeEach
    void criarSessao() throws InterruptedException {
        Thread.sleep(5_000);
        sessaoId = inicializar.executar().id();
    }

    @AfterEach
    void limpar() {
        registroJpa.findBySessaoIdOrderByRegistradoEmDesc(sessaoId).forEach(registroJpa::delete);
        listaJpa.findBySessaoId(sessaoId).ifPresent(listaJpa::delete);
        sessaoJpa.deleteById(sessaoId);
    }

    private ItemRoteiroDetalhadoResponse adicionarAoRoteiro(String sku) {
        Produto produto = produtoRepository.buscarPorSku(sku).orElseThrow();
        return adicionar.executar(sessaoId, produto.getId());
    }

    @Test
    void elegeOSubstitutoSemanticoEntreOsVizinhosDisponiveis() {
        ItemRoteiroDetalhadoResponse item = adicionarAoRoteiro(LIXA_EM_FALTA);

        RupturaEstoqueResponse resposta = tratarRuptura.executar(item.id());

        System.out.println(">>> em falta:   " + item.produto().nome());
        System.out.println(">>> sugerido:   " + resposta.produtoSugerido().nome()
                + " (" + resposta.produtoSugerido().sku() + ")");
        System.out.println(">>> corredor:   " + resposta.produtoSugerido().pontoMapa().corredor());
        System.out.println(">>> origem:     " + resposta.origemSugestao());
        System.out.println(">>> justifica.: " + resposta.justificativa());

        assertThat(resposta.origemSugestao())
                .as("se cair em PROXIMIDADE, o assistente nao respondeu - verifique a cota da chave")
                .isEqualTo(OrigemSugestao.ASSISTENTE_IA);

        assertThat(resposta.produtoSugerido().sku())
                .as("no mesmo raio ha disjuntor, cabo e parafuso; so a lixa d'agua cumpre a funcao")
                .isEqualTo(LIXA_SUBSTITUTA);

        assertThat(resposta.produtoSugerido().saldoEstoque()).isPositive();
        assertThat(resposta.justificativa()).isNotBlank();
    }

    @Test
    void registraARupturaParaAOperacaoDaLoja() {
        ItemRoteiroDetalhadoResponse item = adicionarAoRoteiro(LIXA_EM_FALTA);

        tratarRuptura.executar(item.id());

        List<RegistroRuptura> registros = registroRupturaRepository.buscarPorSessao(sessaoId);

        assertThat(registros).hasSize(1);
        assertThat(registros.getFirst().getItemRoteiroId()).isEqualTo(item.id());
        assertThat(registros.getFirst().temSugestao()).isTrue();
        assertThat(registros.getFirst().getJustificativa()).isNotBlank();

        System.out.println(">>> registro persistido: " + registros.getFirst().getOrigem()
                + " em " + registros.getFirst().getRegistradoEm());
    }

    @Test
    void produtoIsoladoNaLojaNaoRendeSugestaoENemDeixaDeSerRegistrado() {
        // O espelho de Decoracao esta em (88,55), a mais de 25 unidades de qualquer outra
        // secao: nao ha vizinho dentro do raio, e a ruptura precisa ser registrada mesmo assim.
        ItemRoteiroDetalhadoResponse item = adicionarAoRoteiro("SKU-DEC-001");

        assertThatThrownBy(() -> tratarRuptura.executar(item.id()))
                .isInstanceOf(SubstitutoIndisponivelException.class);

        List<RegistroRuptura> registros = registroRupturaRepository.buscarPorSessao(sessaoId);

        assertThat(registros).hasSize(1);
        assertThat(registros.getFirst().temSugestao()).isFalse();
        assertThat(registros.getFirst().getOrigem()).isEqualTo(OrigemSugestao.NENHUMA);
    }
}
