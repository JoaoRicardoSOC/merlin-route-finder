package br.com.jence.backend.service;

import br.com.jence.backend.service.ExpirarSessoesInativasUseCase.ResultadoDaVarredura;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.ConexaoFalsa;
import br.com.jence.backend.factory.ConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpirarSessoesInativasUseCaseTest {

    /** Devolve as sessoes vencidas combinadas para a referencia e anota cada gravacao. */
    static class SessaoDaoFalso extends SessaoDao {
        final Map<LocalDateTime, List<Sessao>> expiradas = new HashMap<>();
        final List<Sessao> salvas = new ArrayList<>();

        @Override
        public List<Sessao> buscarExpiradas(LocalDateTime referencia) {
            return expiradas.getOrDefault(referencia, List.of());
        }

        @Override
        public Sessao salvar(Sessao sessao) {
            salvas.add(sessao);
            return sessao;
        }
    }

    /** Listas por sessao em memoria; as sessoes marcadas como falhando lancam na leitura. */
    static class ListaRoteiroDaoFalso extends ListaRoteiroDao {
        final Map<UUID, ListaRoteiro> listas = new HashMap<>();
        final Set<UUID> falhando = new HashSet<>();

        @Override
        public Optional<ListaRoteiro> buscarPorSessao(UUID sessaoId) {
            if (falhando.contains(sessaoId)) {
                throw new IllegalStateException("banco momentaneamente fora");
            }
            return Optional.ofNullable(listas.get(sessaoId));
        }
    }

    private SessaoDaoFalso sessaoDao;
    private ListaRoteiroDaoFalso listaRoteiroDao;
    private ExpirarSessoesInativasUseCase useCase;

    @BeforeEach
    void preparar() {
        ConexaoFalsa.usarNaFabrica();
        sessaoDao = new SessaoDaoFalso();
        listaRoteiroDao = new ListaRoteiroDaoFalso();
        useCase = new ExpirarSessoesInativasUseCase(sessaoDao, listaRoteiroDao);
    }

    @AfterEach
    void restaurar() {
        ConnectionFactory.usarBanco();
    }

    private static final LocalDateTime AGORA = LocalDateTime.of(2026, 8, 22, 20, 0);

    // ---------------------------------------------------------------- montagem do cenario

    /** Sessao ACTIVE cujo TTL venceu ha meia hora. */
    private Sessao sessaoVencida() {
        return Sessao.reconstituir(UUID.randomUUID(), StatusSessao.ACTIVE,
                AGORA.minusHours(2), AGORA.minusMinutes(30), null, null);
    }

    private void aVarreduraEncontra(Sessao... sessoes) {
        sessaoDao.expiradas.put(AGORA, List.of(sessoes));
    }

    private void comCarrinhoMontado(Sessao sessao) {
        ItemRoteiro item = new ItemRoteiro(UUID.randomUUID(), new Produto(
                UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L",
                new BigDecimal("289.90"), 12,
                new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10)));

        listaRoteiroDao.listas.put(sessao.getId(),
                ListaRoteiro.reconstituir(UUID.randomUUID(), sessao.getId(), List.of(item)));
    }

    private void comCarrinhoVazio(Sessao sessao) {
        listaRoteiroDao.listas.put(sessao.getId(),
                ListaRoteiro.criarPara(UUID.randomUUID(), sessao.getId()));
    }

    // ---------------------------------------------------------------- os dois desfechos

    @Test
    @DisplayName("lista montada vira ABANDONED: e um carrinho abandonado, nao um bounce")
    void carrinhoMontadoViraAbandonada() throws Exception {
        Sessao sessao = sessaoVencida();
        aVarreduraEncontra(sessao);
        comCarrinhoMontado(sessao);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertEquals(StatusSessao.ABANDONED, sessao.getStatus());
        assertEquals(1, resultado.abandonadas());
        assertEquals(0, resultado.expiradas());
        assertEquals(List.of(sessao), sessaoDao.salvas);
    }

    @Test
    @DisplayName("lista vazia vira EXPIRED: abriu a pagina e foi embora sem escolher nada")
    void carrinhoVazioViraExpirada() throws Exception {
        Sessao sessao = sessaoVencida();
        aVarreduraEncontra(sessao);
        comCarrinhoVazio(sessao);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertEquals(StatusSessao.EXPIRED, sessao.getStatus());
        assertEquals(1, resultado.expiradas());
        assertEquals(0, resultado.abandonadas());
    }

    @Test
    @DisplayName("sessao sem lista alguma tambem vira EXPIRED")
    void semListaViraExpirada() throws Exception {
        Sessao sessao = sessaoVencida();
        aVarreduraEncontra(sessao);
        // nenhuma lista cadastrada para ela

        useCase.executar(AGORA);

        assertEquals(StatusSessao.EXPIRED, sessao.getStatus());
    }

    @Test
    @DisplayName("as duas classificacoes convivem na mesma varredura")
    void classificaCadaSessaoPorConta() throws Exception {
        Sessao comItens = sessaoVencida();
        Sessao vazia = sessaoVencida();
        aVarreduraEncontra(comItens, vazia);
        comCarrinhoMontado(comItens);
        comCarrinhoVazio(vazia);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertEquals(StatusSessao.ABANDONED, comItens.getStatus());
        assertEquals(StatusSessao.EXPIRED, vazia.getStatus());
        assertEquals(2, resultado.total());
    }

    // ---------------------------------------------------------------- o que nao pode acontecer

    @Test
    @DisplayName("sessao ja concluida nao e sobrescrita, mesmo com o TTL vencido")
    void naoSobrescreveSessaoConcluida() throws Exception {
        /*
         * O caso que a D-06 previu: o cliente conclui a rota as 10h00 e as 10h05 o cron acha a
         * sessao com TTL vencido. Sobrescrever para ABANDONED apagaria a informacao de que a
         * jornada foi completada - que e a metrica de sucesso do produto.
         */
        Sessao concluida = Sessao.reconstituir(UUID.randomUUID(), StatusSessao.COMPLETED,
                AGORA.minusHours(2), AGORA.minusMinutes(30), null, null);
        aVarreduraEncontra(concluida);
        comCarrinhoMontado(concluida);

        useCase.executar(AGORA);

        assertEquals(StatusSessao.COMPLETED, concluida.getStatus(),
                "o guard da D-06 protege o desfecho que ja aconteceu");
    }

    @Test
    @DisplayName("varredura sem sessoes vencidas nao grava nada")
    void nadaAFazer() throws Exception {
        sessaoDao.expiradas.put(AGORA, List.of());

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertTrue(resultado.vazio());
        assertTrue(sessaoDao.salvas.isEmpty());
    }

    @Test
    @DisplayName("uma sessao que falha nao interrompe a varredura das outras")
    void falhaIsoladaNaoDerrubaAVarredura() throws Exception {
        Sessao problematica = sessaoVencida();
        Sessao saudavel = sessaoVencida();
        aVarreduraEncontra(problematica, saudavel);

        listaRoteiroDao.falhando.add(problematica.getId());
        comCarrinhoVazio(saudavel);

        ResultadoDaVarredura resultado = useCase.executar(AGORA);

        assertEquals(1, resultado.falhas());
        assertEquals(StatusSessao.EXPIRED, saudavel.getStatus(),
                "a segunda sessao precisa ser tratada apesar da falha na primeira");
        assertEquals(1, sessaoDao.salvas.stream().filter(s -> s == saudavel).count());
        assertFalse(sessaoDao.salvas.contains(problematica));
    }
}
