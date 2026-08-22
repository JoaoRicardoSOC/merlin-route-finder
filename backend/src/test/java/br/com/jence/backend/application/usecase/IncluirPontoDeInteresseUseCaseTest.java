package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.PontoRotaResponse;
import br.com.jence.backend.application.dto.RotaCalculadaResponse;
import br.com.jence.backend.domain.entity.*;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Testes da logica de insercao do desvio. Sem banco e sem Spring. */
@ExtendWith(MockitoExtension.class)
class IncluirPontoDeInteresseUseCaseTest {

    @Mock ListaRoteiroRepository listaRoteiroRepository;
    @Mock PontoMapaRepository pontoMapaRepository;
    @InjectMocks IncluirPontoDeInteresseUseCase useCase;

    private static final PontoMapa MATERIAIS = ponto(TipoPonto.PRATELEIRA, "Materiais de construcao", 14, 80);
    private static final PontoMapa JARDIM = ponto(TipoPonto.PRATELEIRA, "Jardim", 36, 50);
    private static final PontoMapa ENCANAMENTO = ponto(TipoPonto.PRATELEIRA, "Encanamento", 48, 30);
    private static final PontoMapa TINTAS = ponto(TipoPonto.PRATELEIRA, "Tintas", 32, 10);

    private static final PontoMapa BANHEIRO_PERTO = ponto(TipoPonto.BANHEIRO, "Sanitarios Fundo", 45, 35);
    private static final PontoMapa BANHEIRO_LONGE = ponto(TipoPonto.BANHEIRO, "Sanitarios Entrada", 52, 92);

    private UUID sessaoId;
    private List<ItemRoteiro> itens;

    private static PontoMapa ponto(TipoPonto tipo, String corredor, int x, int y) {
        return new PontoMapa(UUID.randomUUID(), tipo, corredor, x, y);
    }

    private static ItemRoteiro item(String nome, PontoMapa ponto, int ordem) {
        Produto p = new Produto(UUID.randomUUID(), "SKU-" + nome, nome, new BigDecimal("10.00"), 5, ponto);
        ItemRoteiro i = new ItemRoteiro(UUID.randomUUID(), p);
        i.definirOrdem(ordem);
        return i;
    }

    @BeforeEach
    void montarRotaDeQuatroParadas() {
        sessaoId = UUID.randomUUID();
        itens = List.of(
                item("Argamassa", MATERIAIS, 1),
                item("Vaso", JARDIM, 2),
                item("Cano", ENCANAMENTO, 3),
                item("Tinta", TINTAS, 4));
    }

    private void comLista() {
        ListaRoteiro lista = ListaRoteiro.reconstituir(UUID.randomUUID(), sessaoId, itens, null, null);
        when(listaRoteiroRepository.buscarPorSessao(sessaoId)).thenReturn(Optional.of(lista));
    }

    private List<String> corredoresDa(RotaCalculadaResponse rota) {
        return rota.pontos().stream().map(p -> p.pontoMapa().corredor()).toList();
    }

    @Test
    @DisplayName("sem nada coletado, o desvio entra no inicio do trajeto")
    void desvioNoInicio() {
        comLista();
        when(pontoMapaRepository.buscarPorTipo(TipoPonto.BANHEIRO))
                .thenReturn(List.of(BANHEIRO_PERTO, BANHEIRO_LONGE));

        RotaCalculadaResponse rota = useCase.executar(sessaoId, TipoPonto.BANHEIRO);

        assertThat(corredoresDa(rota).get(0)).startsWith("Sanitarios");
        assertThat(rota.pontos()).hasSize(5);
        assertThat(rota.pontos()).extracting(PontoRotaResponse::ordem).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("com 2 de 4 coletados, o desvio entra entre o coletado e o proximo")
    void desvioNoMeio() {
        itens.get(0).marcarComoColetado();
        itens.get(1).marcarComoColetado();
        comLista();
        when(pontoMapaRepository.buscarPorTipo(TipoPonto.BANHEIRO))
                .thenReturn(List.of(BANHEIRO_PERTO, BANHEIRO_LONGE));

        RotaCalculadaResponse rota = useCase.executar(sessaoId, TipoPonto.BANHEIRO);

        System.out.println(">>> rota com desvio: " + corredoresDa(rota));

        assertThat(corredoresDa(rota))
                .as("o banheiro entra apos o ultimo coletado (Jardim), antes de Encanamento")
                .containsExactly("Materiais de construcao", "Jardim", "Sanitarios Fundo", "Encanamento", "Tintas");
        assertThat(rota.pontos()).extracting(PontoRotaResponse::ordem).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("escolhe o ponto de apoio mais proximo da posicao atual")
    void escolheOMaisProximo() {
        itens.get(0).marcarComoColetado();
        itens.get(1).marcarComoColetado();
        comLista();
        when(pontoMapaRepository.buscarPorTipo(TipoPonto.BANHEIRO))
                .thenReturn(List.of(BANHEIRO_LONGE, BANHEIRO_PERTO));

        RotaCalculadaResponse rota = useCase.executar(sessaoId, TipoPonto.BANHEIRO);

        // Do Jardim (36,50): "Fundo" (45,35) esta a ~17 e "Entrada" (52,92) a ~45.
        assertThat(corredoresDa(rota)).contains("Sanitarios Fundo").doesNotContain("Sanitarios Entrada");
    }

    @Test
    @DisplayName("o ponto de apoio vem sem item, e os itens de compra mantem o seu")
    void pontoDeApoioNaoTemItem() {
        comLista();
        when(pontoMapaRepository.buscarPorTipo(TipoPonto.CAIXA))
                .thenReturn(List.of(ponto(TipoPonto.CAIXA, "Frente de loja", 62, 88)));

        RotaCalculadaResponse rota = useCase.executar(sessaoId, TipoPonto.CAIXA);

        PontoRotaResponse apoio = rota.pontos().stream()
                .filter(p -> p.pontoMapa().tipo() == TipoPonto.CAIXA).findFirst().orElseThrow();
        assertThat(apoio.item()).as("ponto de apoio nao tem produto a coletar").isNull();

        assertThat(rota.pontos().stream().filter(p -> p.pontoMapa().tipo() == TipoPonto.PRATELEIRA))
                .allSatisfy(p -> assertThat(p.item()).isNotNull());
    }

    @Test
    @DisplayName("a ordem de caminho gravada nos itens nao e alterada")
    void naoAlteraAOrdemDosItens() {
        itens.get(0).marcarComoColetado();
        comLista();
        when(pontoMapaRepository.buscarPorTipo(TipoPonto.BANHEIRO)).thenReturn(List.of(BANHEIRO_PERTO));

        useCase.executar(sessaoId, TipoPonto.BANHEIRO);

        assertThat(itens).extracting(ItemRoteiro::getOrdemCaminho).containsExactly(1, 2, 3, 4);
    }

    @Test
    @DisplayName("tipo que nao e de apoio e recusado")
    void recusaTipoInvalido() {
        assertThatThrownBy(() -> useCase.executar(sessaoId, TipoPonto.PRATELEIRA))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("BANHEIRO ou CAIXA");

        assertThatThrownBy(() -> useCase.executar(sessaoId, TipoPonto.TOTEM))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    @DisplayName("sessao sem lista devolve nao encontrado")
    void sessaoSemLista() {
        when(listaRoteiroRepository.buscarPorSessao(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(sessaoId, TipoPonto.BANHEIRO))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    @DisplayName("lista vazia nao tem rota em andamento")
    void listaVazia() {
        ListaRoteiro vazia = ListaRoteiro.criarPara(UUID.randomUUID(), sessaoId);
        when(listaRoteiroRepository.buscarPorSessao(sessaoId)).thenReturn(Optional.of(vazia));

        assertThatThrownBy(() -> useCase.executar(sessaoId, TipoPonto.BANHEIRO))
                .isInstanceOf(OperacaoNaoPermitidaException.class)
                .hasMessageContaining("rota em andamento");
    }
}
