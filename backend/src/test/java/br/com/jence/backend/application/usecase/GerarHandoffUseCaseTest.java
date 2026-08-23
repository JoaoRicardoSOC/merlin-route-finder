package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.HandoffResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.GeradorTokenHandoff;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GerarHandoffUseCaseTest {

    @Mock SessaoRepository sessaoRepository;
    @Mock ListaRoteiroRepository listaRoteiroRepository;
    @Mock PontoMapaRepository pontoMapaRepository;
    @Mock GeradorTokenHandoff geradorTokenHandoff;
    @InjectMocks GerarHandoffUseCase useCase;

    private static final PontoMapa TOTEM =
            new PontoMapa(UUID.randomUUID(), TipoPonto.TOTEM, "Entrada", 50, 95);
    private static final PontoMapa TINTAS =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
    private static final PontoMapa MATERIAIS =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Materiais de construcao", 14, 80);

    private UUID sessaoId;

    @BeforeEach
    void preparar() {
        sessaoId = UUID.randomUUID();
        ReflectionTestUtils.setField(useCase, "baseUrlMobile", "http://localhost:5173");
    }

    private ItemRoteiro item(PontoMapa ponto) {
        return new ItemRoteiro(UUID.randomUUID(), new Produto(
                UUID.randomUUID(), "SKU-" + ponto.getCorredor(), "Produto de " + ponto.getCorredor(),
                new BigDecimal("10.00"), 5, ponto));
    }

    private ListaRoteiro comLista(ItemRoteiro... itens) {
        ListaRoteiro lista = ListaRoteiro.reconstituir(
                UUID.randomUUID(), sessaoId, List.of(itens), null, null);

        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(Sessao.iniciar(sessaoId)));
        when(listaRoteiroRepository.buscarPorSessao(sessaoId)).thenReturn(Optional.of(lista));
        when(geradorTokenHandoff.gerar(any(), any())).thenReturn("token-novo");
        return lista;
    }

    private void comTotemCadastrado() {
        when(pontoMapaRepository.buscarPorTipo(TipoPonto.TOTEM)).thenReturn(List.of(TOTEM));
    }

    @Test
    @DisplayName("gera o token e a URL que o Totem exibe como QR Code")
    void geraOHandoff() {
        comLista(item(TINTAS), item(MATERIAIS));
        comTotemCadastrado();

        HandoffResponse resposta = useCase.executar(sessaoId);

        assertThat(resposta.token()).isEqualTo("token-novo");
        assertThat(resposta.handoffUrl()).isEqualTo("http://localhost:5173/rota?token=token-novo");
        assertThat(resposta.tokenExpiracao()).isNotNull();
    }

    @Test
    @DisplayName("com a caminhada nao iniciada, regerar recalcula a rota")
    void regeneracaoAntesDaCaminhadaRecalcula() {
        // Materiais (14,80) esta mais perto da entrada (50,95) que Tintas (32,10).
        ItemRoteiro tinta = item(TINTAS);
        ItemRoteiro cimento = item(MATERIAIS);
        // Ordem propositalmente errada, como se viesse de um calculo anterior desatualizado.
        tinta.definirOrdem(1);
        cimento.definirOrdem(2);
        comLista(tinta, cimento);
        comTotemCadastrado();

        useCase.executar(sessaoId);

        assertThat(cimento.getOrdemCaminho()).isEqualTo(1);
        assertThat(tinta.getOrdemCaminho()).isEqualTo(2);
    }

    @Test
    @DisplayName("com a caminhada em curso, regerar preserva a ordem das paradas")
    void regeneracaoDuranteACaminhadaPreservaAOrdem() {
        /*
         * O cliente ja esta no meio da loja e perdeu o acesso - fechou a aba, trocou de
         * aparelho, o QR expirou antes de escanear. Recalcular partindo do totem renumeraria
         * paradas que ele ja visitou e embaralharia a navegacao. Regerar devolve o acesso,
         * nao reinicia o percurso.
         */
        ItemRoteiro tinta = item(TINTAS);
        ItemRoteiro cimento = item(MATERIAIS);
        tinta.definirOrdem(1);
        cimento.definirOrdem(2);
        tinta.marcarComoColetado();
        comLista(tinta, cimento);

        HandoffResponse resposta = useCase.executar(sessaoId);

        assertThat(tinta.getOrdemCaminho())
                .as("a parada ja visitada nao pode mudar de lugar")
                .isEqualTo(1);
        assertThat(cimento.getOrdemCaminho()).isEqualTo(2);
        assertThat(tinta.isColetado()).isTrue();
        assertThat(resposta.token())
                .as("o acesso e devolvido mesmo sem recalcular")
                .isEqualTo("token-novo");
    }

    @Test
    @DisplayName("regerar durante a caminhada nao consulta o totem: nao ha rota a recalcular")
    void regeneracaoDuranteACaminhadaNaoProcuraOTotem() {
        ItemRoteiro coletado = item(TINTAS);
        coletado.definirOrdem(1);
        coletado.marcarComoColetado();
        comLista(coletado);

        useCase.executar(sessaoId);

        // O stub de buscarPorTipo nem foi registrado: se fosse chamado, o teste falharia.
        org.mockito.Mockito.verify(pontoMapaRepository, org.mockito.Mockito.never())
                .buscarPorTipo(any());
    }

    @Test
    @DisplayName("o token anterior deixa de valer quando um novo e registrado")
    void tokenAnteriorEhSubstituido() {
        ItemRoteiro tinta = item(TINTAS);
        ListaRoteiro lista = ListaRoteiro.reconstituir(
                UUID.randomUUID(), sessaoId, List.of(tinta), "token-antigo",
                java.time.LocalDateTime.now().plusMinutes(3));

        when(sessaoRepository.buscarPorId(sessaoId)).thenReturn(Optional.of(Sessao.iniciar(sessaoId)));
        when(listaRoteiroRepository.buscarPorSessao(sessaoId)).thenReturn(Optional.of(lista));
        when(geradorTokenHandoff.gerar(any(), any())).thenReturn("token-novo");
        comTotemCadastrado();

        useCase.executar(sessaoId);

        assertThat(lista.getHandoffToken())
                .as("a busca por token nao pode mais encontrar o QR Code antigo")
                .isEqualTo("token-novo");
    }
}
