package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.RupturaEstoqueResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.OrigemSugestao;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.RegistroRuptura;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.domain.repository.ItemRoteiroRepository;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.RegistroRupturaRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.ExecutorDeFerramenta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TratarRupturaEstoqueUseCaseTest {

    @Mock ItemRoteiroRepository itemRoteiroRepository;
    @Mock ListaRoteiroRepository listaRoteiroRepository;
    @Mock SessaoRepository sessaoRepository;
    @Mock ProdutoRepository produtoRepository;
    @Mock RegistroRupturaRepository registroRupturaRepository;
    @Mock AssistenteIA assistenteIA;
    @InjectMocks TratarRupturaEstoqueUseCase useCase;

    private static final PontoMapa TINTAS =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
    private static final PontoMapa ELETRICA =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Eletrica", 34, 30);

    private UUID sessaoId;
    private UUID itemId;
    private Produto lixaEmFalta;
    private Produto lixaDagua;
    private Produto disjuntor;

    @BeforeEach
    void preparar() {
        sessaoId = UUID.randomUUID();
        itemId = UUID.randomUUID();
        lixaEmFalta = produto("SKU-TIN-003", "Lixa para Parede Grao 120", "3.50", 0, TINTAS);
        lixaDagua = produto("SKU-TIN-004", "Lixa d'Agua Grao 150", "4.20", 40, TINTAS);
        disjuntor = produto("SKU-ELE-003", "Disjuntor Bipolar 25A", "42.90", 15, ELETRICA);
    }

    // ---------------------------------------------------------------- montagem do cenario

    private Produto produto(String sku, String nome, String preco, int estoque, PontoMapa ponto) {
        return new Produto(UUID.randomUUID(), sku, nome, new BigDecimal(preco), estoque, ponto);
    }

    /** Roteiro contendo apenas o item cuja prateleira o cliente encontrou vazia. */
    private ItemRoteiro comItemNoRoteiro(Sessao sessao) {
        ItemRoteiro item = ItemRoteiro.reconstituir(itemId, lixaEmFalta, null);
        ListaRoteiro lista = ListaRoteiro.reconstituir(UUID.randomUUID(), sessaoId, List.of(item));

        when(itemRoteiroRepository.buscarPorId(itemId)).thenReturn(Optional.of(item));
        when(listaRoteiroRepository.buscarPorItem(itemId)).thenReturn(Optional.of(lista));
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(sessao));
        return item;
    }

    private ItemRoteiro comSessaoAtiva() {
        return comItemNoRoteiro(Sessao.iniciar(sessaoId));
    }

    /** A consulta espacial devolve, em ordem de distancia, os candidatos informados. */
    private void comCandidatosProximos(Produto... candidatos) {
        when(produtoRepository.buscarDisponiveisProximosDe(
                any(), eq(lixaEmFalta.getId()), any(), anyDouble(), anyInt()))
                .thenReturn(List.of(candidatos));
    }

    private void assistenteResponde(String resposta) {
        when(assistenteIA.conversar(any(), any(), any(), any())).thenReturn(resposta);
    }

    private RegistroRuptura registroSalvo() {
        ArgumentCaptor<RegistroRuptura> captor = ArgumentCaptor.forClass(RegistroRuptura.class);
        verify(registroRupturaRepository).salvar(captor.capture());
        return captor.getValue();
    }

    // ---------------------------------------------------------------- caminho feliz

    @Test
    @DisplayName("o assistente elege um candidato e a sugestao sai com o produto do nosso banco")
    void assistenteElegeSubstituto() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("SKU-TIN-004 | A lixa d'agua grao 150 da o mesmo acabamento e esta no "
                + "mesmo corredor.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertThat(resposta.produtoOriginalId()).isEqualTo(lixaEmFalta.getId());
        assertThat(resposta.produtoSugeridoId()).isEqualTo(lixaDagua.getId());
        assertThat(resposta.produtoSugerido().sku()).isEqualTo("SKU-TIN-004");
        assertThat(resposta.produtoSugerido().preco()).isEqualByComparingTo("4.20");
        assertThat(resposta.justificativa()).startsWith("A lixa d'agua");
        assertThat(resposta.origemSugestao()).isEqualTo(OrigemSugestao.ASSISTENTE_IA);
    }

    @Test
    @DisplayName("o assistente so recebe os candidatos da pre-filtragem espacial")
    void ferramentaDevolveApenasOsCandidatosFiltrados() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("SKU-TIN-004 | serve para o mesmo acabamento");

        useCase.executar(itemId);

        ArgumentCaptor<ExecutorDeFerramenta> captor = ArgumentCaptor.forClass(ExecutorDeFerramenta.class);
        verify(assistenteIA).conversar(any(), any(), any(), captor.capture());

        Object candidatos = captor.getValue().executar("buscar_substitutos_proximos", Map.of()).get("candidatos");

        assertThat(candidatos).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .as("a ferramenta nao pode devolver nada alem do que a consulta espacial trouxe")
                .hasSize(2)
                .allSatisfy(c -> assertThat(((Map<?, ?>) c).get("sku"))
                        .isIn("SKU-TIN-004", "SKU-ELE-003"));

        // A distancia informada ao modelo e a real: mesma secao = 0, Tintas -> Eletrica = 20.
        assertThat(((Map<?, ?>) ((List<?>) candidatos).get(0)).get("distancia")).isEqualTo(0L);
        assertThat(((Map<?, ?>) ((List<?>) candidatos).get(1)).get("distancia")).isEqualTo(20L);
    }

    @Test
    @DisplayName("a ruptura fica registrada com a origem da sugestao")
    void registraARuptura() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua);
        assistenteResponde("SKU-TIN-004 | mesmo acabamento, mesmo corredor");

        useCase.executar(itemId);

        RegistroRuptura registro = registroSalvo();
        assertThat(registro.getSessaoId()).isEqualTo(sessaoId);
        assertThat(registro.getItemRoteiroId()).isEqualTo(itemId);
        assertThat(registro.getProdutoFaltanteId()).isEqualTo(lixaEmFalta.getId());
        assertThat(registro.getProdutoSugeridoId()).isEqualTo(lixaDagua.getId());
        assertThat(registro.getOrigem()).isEqualTo(OrigemSugestao.ASSISTENTE_IA);
        assertThat(registro.temSugestao()).isTrue();
    }

    @Test
    @DisplayName("a sessao e renovada: o cliente esta em plena caminhada pela loja")
    void renovaASessao() {
        Sessao sessao = Sessao.reconstituir(sessaoId, StatusSessao.ACTIVE,
                LocalDateTime.now().minusMinutes(25), LocalDateTime.now().plusMinutes(5), null, null);
        comItemNoRoteiro(sessao);
        comCandidatosProximos(lixaDagua);
        assistenteResponde("SKU-TIN-004 | mesmo acabamento");

        useCase.executar(itemId);

        assertThat(sessao.getExpiracaoTtl()).isAfter(LocalDateTime.now().plusMinutes(25));
        verify(sessaoRepository).salvar(sessao);
    }

    // ---------------------------------------------------------------- grounding

    @Test
    @DisplayName("SKU fora da lista de candidatos e descartado e cai no mais proximo")
    void skuInventadoNaoViraSugestao() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        // Produto que existe no mundo, mas nao estava entre os candidatos que oferecemos.
        assistenteResponde("SKU-MAT-999 | Leve a massa corrida premium, e o que voce precisa.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertThat(resposta.produtoSugeridoId())
                .as("nenhum produto pode sair daqui sem ter vindo da nossa pre-filtragem")
                .isEqualTo(lixaDagua.getId());
        assertThat(resposta.origemSugestao()).isEqualTo(OrigemSugestao.PROXIMIDADE);
        assertThat(resposta.justificativa())
                .as("a justificativa inventada pelo modelo nao pode chegar ao cliente")
                .doesNotContain("massa corrida");
    }

    @Test
    @DisplayName("resposta fora do formato combinado cai no mais proximo")
    void respostaIlegivelCaiNoFallback() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("Acho que voce deveria procurar um vendedor.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertThat(resposta.produtoSugeridoId()).isEqualTo(lixaDagua.getId());
        assertThat(resposta.origemSugestao()).isEqualTo(OrigemSugestao.PROXIMIDADE);
    }

    @Test
    @DisplayName("negrito e crase em volta do SKU nao invalidam a escolha")
    void toleraMarkdownNoCodigo() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("**SKU-TIN-004** | Mesmo acabamento, poucos passos a frente.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertThat(resposta.produtoSugeridoId()).isEqualTo(lixaDagua.getId());
        assertThat(resposta.origemSugestao()).isEqualTo(OrigemSugestao.ASSISTENTE_IA);
    }

    // ---------------------------------------------------------------- degradacao

    @Test
    @DisplayName("assistente fora do ar nao deixa o cliente sem resposta")
    void assistenteIndisponivelCaiNoMaisProximo() {
        comSessaoAtiva();
        comCandidatosProximos(lixaDagua, disjuntor);
        when(assistenteIA.conversar(any(), any(), any(), any()))
                .thenThrow(new AssistenteIAIndisponivelException("cota esgotada"));

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertThat(resposta.produtoSugeridoId()).isEqualTo(lixaDagua.getId());
        assertThat(resposta.origemSugestao()).isEqualTo(OrigemSugestao.PROXIMIDADE);
        assertThat(resposta.justificativa())
                .as("a mensagem precisa ser honesta sobre nao ter havido analise")
                .contains("mais proximo");
        assertThat(registroSalvo().getOrigem()).isEqualTo(OrigemSugestao.PROXIMIDADE);
    }

    // ---------------------------------------------------------------- sem substituto

    @Test
    @DisplayName("sem candidato proximo, registra a ruptura e devolve 422")
    void semCandidatosProximos() {
        ItemRoteiro item = comSessaoAtiva();
        comCandidatosProximos();

        assertThatThrownBy(() -> useCase.executar(itemId))
                .isInstanceOf(SubstitutoIndisponivelException.class)
                .hasMessageContaining("Lixa para Parede Grao 120");

        RegistroRuptura registro = registroSalvo();
        assertThat(registro.temSugestao()).isFalse();
        assertThat(registro.getOrigem()).isEqualTo(OrigemSugestao.NENHUMA);
        assertThat(registro.getProdutoFaltanteId()).isEqualTo(item.getProduto().getId());
        verify(assistenteIA, never()).conversar(any(), any(), any(), any());
    }

    @Test
    @DisplayName("assistente que recusa todos os candidatos nao cai no fallback")
    void assistenteRecusaTodosOsCandidatos() {
        comSessaoAtiva();
        comCandidatosProximos(disjuntor);
        assistenteResponde("NENHUM | Nao ha por perto nenhum item de acabamento equivalente a lixa.");

        assertThatThrownBy(() -> useCase.executar(itemId))
                .as("sugerir algo inadequado e pior do que nao sugerir nada")
                .isInstanceOf(SubstitutoIndisponivelException.class);

        RegistroRuptura registro = registroSalvo();
        assertThat(registro.temSugestao()).isFalse();
        assertThat(registro.getJustificativa()).contains("item de acabamento equivalente");
    }

    @Test
    @DisplayName("produto que o cliente ja vai levar nao entra como candidato")
    void ignoraCandidatoQueJaEstaNoRoteiro() {
        ItemRoteiro item = ItemRoteiro.reconstituir(itemId, lixaEmFalta, null);
        ItemRoteiro jaNoCarrinho = ItemRoteiro.reconstituir(UUID.randomUUID(), lixaDagua, null);
        ListaRoteiro lista = ListaRoteiro.reconstituir(
                UUID.randomUUID(), sessaoId, List.of(item, jaNoCarrinho));

        when(itemRoteiroRepository.buscarPorId(itemId)).thenReturn(Optional.of(item));
        when(listaRoteiroRepository.buscarPorItem(itemId)).thenReturn(Optional.of(lista));
        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(Sessao.iniciar(sessaoId)));
        comCandidatosProximos(lixaDagua, disjuntor);
        assistenteResponde("SKU-ELE-003 | Nao ha lixa equivalente; leve o disjuntor.");

        RupturaEstoqueResponse resposta = useCase.executar(itemId);

        assertThat(resposta.produtoSugeridoId())
                .as("a lixa d'agua ja esta no carrinho e nao pode ser oferecida de novo")
                .isEqualTo(disjuntor.getId());
    }

    // ---------------------------------------------------------------- pre-condicoes

    @Test
    @DisplayName("item inexistente devolve 404")
    void itemInexistente() {
        when(itemRoteiroRepository.buscarPorId(itemId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(itemId))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(registroRupturaRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("sessao encerrada devolve 409 e nao consulta a IA")
    void sessaoInativa() {
        Sessao encerrada = Sessao.iniciar(sessaoId);
        encerrada.encerrar();
        comItemNoRoteiro(encerrada);

        assertThatThrownBy(() -> useCase.executar(itemId))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("COMPLETED");

        verify(assistenteIA, never()).conversar(any(), any(), any(), any());
        verify(registroRupturaRepository, never()).salvar(any());
    }
}
