package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.usecase.ExpirarSessoesInativasUseCase.ResultadoDaVarredura;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpirarSessoesInativasUseCaseTest {

    @Mock SessaoRepository sessaoRepository;
    @Mock ListaRoteiroRepository listaRoteiroRepository;
    @InjectMocks ExpirarSessoesInativasUseCase useCase;

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 22, 20, 0);

    // ---------------------------------------------------------------- montagem do cenario

    /** Sessao ACTIVE cujo TTL venceu ha meia hora. */
    private Sessao sessaoVencida() {
        return Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                AGORA.minusHours(2), AGORA.minusMinutes(30));
    }

    private void aVarreduraEncontra(Sessao... sessoes) {
        when(sessaoRepository.buscarExpiradas(AGORA)).thenReturn(List.of(sessoes));
    }

    private void comCarrinhoMontado(Sessao sessao) {
        ItemRoteiro item = new ItemRoteiro(UUID.randomUUID(), new Produto(
                UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                new BigDecimal("289.90"), 12,
                new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10)));

        when(listaRoteiroRepository.buscarPorSessao(sessao.getId())).thenReturn(
                Optional.of(ListaRoteiro.reconstituir(
                        UUID.randomUUID(), sessao.getId(), List.of(item))));
    }

    private void comCarrinhoVazio(Sessao sessao) {
        when(listaRoteiroRepository.buscarPorSessao(sessao.getId())).thenReturn(
                Optional.of(ListaRoteiro.criarPara(UUID.randomUUID(), sessao.getId())));
    }

    // ---------------------------------------------------------------- os dois desfechos

    @Test
    @DisplayName("lista montada vira ABANDONED: e um carrinho abandonado, nao um bounce")
    void carrinhoMontadoViraAbandonada() {
        Sessao sessao = sessaoVencida();
        aVarreduraEncontra(sessao);
        comCarrinhoMontado(sessao);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.ABANDONED);
        assertThat(resultado.abandonadas()).isEqualTo(1);
        assertThat(resultado.expiradas()).isZero();
        verify(sessaoRepository).salvar(sessao);
    }

    @Test
    @DisplayName("lista vazia vira EXPIRED: encostou no totem e foi embora")
    void carrinhoVazioViraExpirada() {
        Sessao sessao = sessaoVencida();
        aVarreduraEncontra(sessao);
        comCarrinhoVazio(sessao);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.EXPIRED);
        assertThat(resultado.expiradas()).isEqualTo(1);
        assertThat(resultado.abandonadas()).isZero();
    }

    @Test
    @DisplayName("sessao sem lista alguma tambem vira EXPIRED")
    void semListaViraExpirada() {
        Sessao sessao = sessaoVencida();
        aVarreduraEncontra(sessao);
        when(listaRoteiroRepository.buscarPorSessao(sessao.getId())).thenReturn(Optional.empty());

        useCase.executar(AGORA);

        assertThat(sessao.getStatus()).isEqualTo(StatusSessao.EXPIRED);
    }

    @Test
    @DisplayName("as duas classificacoes convivem na mesma varredura")
    void classificaCadaSessaoPorConta() {
        Sessao comItens = sessaoVencida();
        Sessao vazia = sessaoVencida();
        aVarreduraEncontra(comItens, vazia);
        comCarrinhoMontado(comItens);
        comCarrinhoVazio(vazia);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertThat(comItens.getStatus()).isEqualTo(StatusSessao.ABANDONED);
        assertThat(vazia.getStatus()).isEqualTo(StatusSessao.EXPIRED);
        assertThat(resultado.total()).isEqualTo(2);
    }

    // ---------------------------------------------------------------- o que nao pode acontecer

    @Test
    @DisplayName("sessao ja concluida nao e sobrescrita, mesmo com o TTL vencido")
    void naoSobrescreveSessaoConcluida() {
        /*
         * O caso que a D-06 previu: o cliente conclui a rota as 10h00 e as 10h05 o cron acha a
         * sessao com TTL vencido. Sobrescrever para ABANDONED apagaria a informacao de que a
         * jornada foi completada - que e a metrica de sucesso do produto.
         */
        Sessao concluida = Sessao.reconstituir(UUID.randomUUID(), StatusSessao.COMPLETED,
                AGORA.minusHours(2), AGORA.minusMinutes(30));
        aVarreduraEncontra(concluida);
        comCarrinhoMontado(concluida);

        useCase.executar(AGORA);

        assertThat(concluida.getStatus())
                .as("o guard da D-06 protege o desfecho que ja aconteceu")
                .isEqualTo(StatusSessao.COMPLETED);
    }

    @Test
    @DisplayName("varredura sem sessoes vencidas nao grava nada")
    void nadaAFazer() {
        when(sessaoRepository.buscarExpiradas(AGORA)).thenReturn(List.of());

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertThat(resultado.vazio()).isTrue();
        verify(sessaoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("uma sessao que falha nao interrompe a varredura das outras")
    void falhaIsoladaNaoDerrubaAVarredura() {
        Sessao problematica = sessaoVencida();
        Sessao saudavel = sessaoVencida();
        aVarreduraEncontra(problematica, saudavel);

        when(listaRoteiroRepository.buscarPorSessao(problematica.getId()))
                .thenThrow(new IllegalStateException("banco momentaneamente fora"));
        comCarrinhoVazio(saudavel);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertThat(resultado.falhas()).isEqualTo(1);
        assertThat(saudavel.getStatus())
                .as("a segunda sessao precisa ser tratada apesar da falha na primeira")
                .isEqualTo(StatusSessao.EXPIRED);
        verify(sessaoRepository).salvar(saudavel);
        verify(sessaoRepository, never()).salvar(problematica);
    }
}
