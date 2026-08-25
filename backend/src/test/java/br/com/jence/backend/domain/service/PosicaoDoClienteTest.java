package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.entity.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Onde o cliente esta, deduzido das duas unicas pistas que existem sem GPS dentro da loja:
 * a placa que ele leu e o ultimo item que ele pegou.
 */
class PosicaoDoClienteTest {

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 25, 10, 0);

    private static final PontoMapa PLACA_ENTRADA = new PontoMapa(
            UUID.randomUUID(), TipoPonto.QR_CODE, "Entrada da loja", 50, 92, "ENT-01");
    private static final PontoMapa PLACA_CENTRAL = new PontoMapa(
            UUID.randomUUID(), TipoPonto.QR_CODE, "Cruzamento central", 41, 40, "CEN-03");
    private static final PontoMapa PRATELEIRA_TINTAS = new PontoMapa(
            UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
    private static final PontoMapa PRATELEIRA_JARDIM = new PontoMapa(
            UUID.randomUUID(), TipoPonto.PRATELEIRA, "Jardim", 36, 50);

    // ---------------------------------------------------------------- montagem

    private Sessao sessaoQueLeu(PontoMapa placa, LocalDateTime quando) {
        return Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                AGORA.minusHours(1), AGORA.plusHours(3), placa, quando);
    }

    private Sessao sessaoSemPlaca() {
        return Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                AGORA.minusHours(1), AGORA.plusHours(3), null, null);
    }

    private ItemRoteiro item(PontoMapa onde, LocalDateTime coletadoEm) {
        Produto produto = new Produto(UUID.randomUUID(), "SKU-" + UUID.randomUUID(),
                "Produto de " + onde.getCorredor(), new BigDecimal("10.00"), 5, onde);
        return ItemRoteiro.reconstituir(UUID.randomUUID(), produto, coletadoEm);
    }

    private ListaRoteiro listaCom(ItemRoteiro... itens) {
        return ListaRoteiro.reconstituir(UUID.randomUUID(), UUID.randomUUID(), List.of(itens));
    }

    // ---------------------------------------------------------------- as duas pistas isoladas

    @Test
    @DisplayName("sem placa e sem nada coletado, nao ha pista alguma")
    void semPista() {
        /*
         * Nao e erro: o cliente pode abrir a pagina sem ler placa nenhuma. O mapa aparece sem
         * "voce esta aqui", que e melhor do que recusar a sessao.
         */
        assertThat(PosicaoDoCliente.estimar(sessaoSemPlaca(), listaCom())).isEmpty();
    }

    @Test
    @DisplayName("so a placa lida: e onde ele esta")
    void apenasAPlaca() {
        assertThat(PosicaoDoCliente.estimar(sessaoQueLeu(PLACA_ENTRADA, AGORA), listaCom()))
                .contains(PLACA_ENTRADA);
    }

    @Test
    @DisplayName("so um item coletado: ele esta na prateleira daquele produto")
    void apenasAColeta() {
        Sessao sessao = sessaoSemPlaca();
        ListaRoteiro lista = listaCom(item(PRATELEIRA_TINTAS, AGORA));

        assertThat(PosicaoDoCliente.estimar(sessao, lista)).contains(PRATELEIRA_TINTAS);
    }

    @Test
    @DisplayName("itens ainda nao coletados nao dizem nada sobre onde ele esta")
    void itensNaoColetadosNaoContam() {
        Sessao sessao = sessaoQueLeu(PLACA_ENTRADA, AGORA);
        ListaRoteiro lista = listaCom(item(PRATELEIRA_TINTAS, null), item(PRATELEIRA_JARDIM, null));

        assertThat(PosicaoDoCliente.estimar(sessao, lista))
                .as("ter o produto na lista nao significa ter passado por ele")
                .contains(PLACA_ENTRADA);
    }

    // ---------------------------------------------------------------- a ordem entre coletas

    @Test
    @DisplayName("entre varios coletados, vale o mais recente")
    void oUltimoColetadoVence() {
        /*
         * E por isso que ItemRoteiro guarda um instante e nao um booleano: ate a Fase 3 a
         * ordem vinha do campo de rota, que saiu junto com o calculo de rota.
         */
        Sessao sessao = sessaoSemPlaca();
        ListaRoteiro lista = listaCom(
                item(PRATELEIRA_TINTAS, AGORA.minusMinutes(20)),
                item(PRATELEIRA_JARDIM, AGORA.minusMinutes(2)));

        assertThat(PosicaoDoCliente.estimar(sessao, lista)).contains(PRATELEIRA_JARDIM);
    }

    @Test
    @DisplayName("a ordem da lista nao influencia: quem decide e a hora")
    void ordemDaListaNaoImporta() {
        Sessao sessao = sessaoSemPlaca();
        ListaRoteiro lista = listaCom(
                item(PRATELEIRA_JARDIM, AGORA.minusMinutes(2)),
                item(PRATELEIRA_TINTAS, AGORA.minusMinutes(20)));

        assertThat(PosicaoDoCliente.estimar(sessao, lista)).contains(PRATELEIRA_JARDIM);
    }

    // ---------------------------------------------------------------- placa contra coleta

    @Test
    @DisplayName("placa lida depois da ultima coleta vence: e o recentrar funcionando")
    void placaMaisRecenteVence() {
        /*
         * O caso que justifica comparar datas em vez de dar preferencia fixa a uma das pistas.
         * O cliente pegou a tinta, andou, se perdeu e leu a placa do cruzamento. Ele espera
         * ver o marcador no cruzamento - nao de volta em Tintas.
         */
        Sessao sessao = sessaoQueLeu(PLACA_CENTRAL, AGORA.minusMinutes(1));
        ListaRoteiro lista = listaCom(item(PRATELEIRA_TINTAS, AGORA.minusMinutes(15)));

        assertThat(PosicaoDoCliente.estimar(sessao, lista)).contains(PLACA_CENTRAL);
    }

    @Test
    @DisplayName("coleta depois da placa vence: ele saiu da placa e foi buscar o produto")
    void coletaMaisRecenteVence() {
        Sessao sessao = sessaoQueLeu(PLACA_ENTRADA, AGORA.minusMinutes(30));
        ListaRoteiro lista = listaCom(item(PRATELEIRA_TINTAS, AGORA.minusMinutes(3)));

        assertThat(PosicaoDoCliente.estimar(sessao, lista)).contains(PRATELEIRA_TINTAS);
    }

    // ---------------------------------------------------------------- bordas

    @Test
    @DisplayName("lista ausente nao quebra a estimativa")
    void semLista() {
        // Acontece na resposta de POST /sessoes, onde a lista acabou de nascer vazia.
        assertThat(PosicaoDoCliente.estimar(sessaoQueLeu(PLACA_ENTRADA, AGORA), null))
                .contains(PLACA_ENTRADA);
    }
}
