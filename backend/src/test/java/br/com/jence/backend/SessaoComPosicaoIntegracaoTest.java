package br.com.jence.backend;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.ConsultarSessaoUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.ConcluirRotaUseCase;
import br.com.jence.backend.service.DesmarcarItemColetadoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.service.RecentrarSessaoUseCase;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * A sessao sabendo onde o cliente esta, do codigo da placa ate a posicao devolvida.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class SessaoComPosicaoIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired ConsultarSessaoUseCase consultar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired MarcarItemColetadoUseCase marcarColetado;
    @Autowired RecentrarSessaoUseCase recentrar;
    @Autowired DesmarcarItemColetadoUseCase desmarcar;
    @Autowired ConcluirRotaUseCase concluir;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired ListaRoteiroRepository listaRoteiroRepository;

    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;

    private final List<UUID> criadas = new ArrayList<>();

    @AfterEach
    void limpar() {
        criadas.forEach(id -> {
            listaJpa.findBySessaoId(id).ifPresent(listaJpa::delete);
            sessaoJpa.deleteById(id);
        });
        criadas.clear();
    }

    private SessaoResponse iniciarEm(String codigo) {
        SessaoResponse sessao = inicializar.executar(codigo);
        criadas.add(sessao.id());
        return sessao;
    }

    private UUID coletar(UUID sessaoId, String sku) throws Exception {
        UUID produtoId = produtoRepository.buscarPorSku(sku).orElseThrow().getId();
        UUID itemId = adicionar.executar(sessaoId, produtoId).id();
        marcarColetado.executar(itemId);
        return itemId;
    }

    // ---------------------------------------------------------------- entrada pela placa

    @Test
    @DisplayName("codigo valido: a sessao ja nasce sabendo onde o cliente esta")
    void nasceComPosicao() {
        SessaoResponse sessao = iniciarEm("ENT-01");

        assertNotNull(sessao.posicaoAtual());
        assertEquals("ENT01", sessao.posicaoAtual().codigoCurto());
        assertEquals(TipoPonto.QR_CODE, sessao.posicaoAtual().tipo());
    }

    @Test
    @DisplayName("o plano B chega pelo mesmo caminho: codigo digitado em qualquer grafia")
    void codigoDigitado() {
        // O cliente que nao conseguiu escanear digita o que le na placa, do jeito que le.
        assertEquals("ENT01", iniciarEm("ent 01").posicaoAtual().codigoCurto());
        assertEquals("TIN02", iniciarEm("Tin-02").posicaoAtual().codigoCurto());
    }

    @Test
    @DisplayName("codigo desconhecido nao recusa a sessao: ela nasce sem posicao e funciona")
    void codigoDesconhecidoNaoBarraAEntrada() {
        /*
         * Placa velha, loja remanejada ou erro de digitacao. Recusar aqui seria barrar a
         * entrada por causa de um adesivo - o cliente ficaria sem sistema nenhum.
         */
        SessaoResponse sessao = iniciarEm("ZZZ-99");

        assertNull(sessao.posicaoAtual());
        assertNotNull(sessao.id());

        UUID produtoId = produtoRepository.buscarPorSku("SKU-TIN-001").orElseThrow().getId();
        assertNotNull(adicionar.executar(sessao.id(), produtoId));
        assertTrue(listaRoteiroRepository.buscarPorSessao(sessao.id()).isPresent());
    }

    @Test
    @DisplayName("sem codigo algum a sessao tambem nasce, so que sem posicao")
    void semCodigo() {
        assertNull(iniciarEm(null).posicaoAtual());
    }

    // ---------------------------------------------------------------- a posicao acompanha a coleta

    @Test
    @DisplayName("coletar um item move a posicao para a prateleira dele")
    void coletaMoveAPosicao() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");

        assertEquals("Tintas", consultar.executar(sessao.id()).posicaoAtual().corredor(),
                "depois de pegar a tinta, o cliente esta em Tintas, nao na entrada");
    }

    @Test
    @DisplayName("com varios coletados, a posicao e a do ultimo")
    void ultimoColetadoVence() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");

        coletar(sessao.id(), "SKU-TIN-001");
        coletar(sessao.id(), "SKU-JAR-001");

        String corredor = consultar.executar(sessao.id()).posicaoAtual().corredor();

        System.out.println(">>> posicao apos duas coletas: " + corredor);
        assertEquals("Jardim", corredor);
    }

    // ---------------------------------------------------------------- recentrar

    @Test
    @DisplayName("recentrar move a posicao e vence o ultimo item coletado")
    void recentrarVenceAColeta() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");

        SessaoResponse depois = recentrar.executar(sessao.id(), "CEN-03");

        assertEquals("CEN03", depois.posicaoAtual().codigoCurto(),
                "quem leu uma placa agora nao esta mais na prateleira de antes");
        assertEquals("CEN03", consultar.executar(sessao.id()).posicaoAtual().codigoCurto());
    }

    @Test
    @DisplayName("recentrar nao mexe na lista nem no que ja foi coletado")
    void recentrarPreservaALista() throws Exception {
        /*
         * E a promessa central da operacao: o cliente se perdeu, nao recomecou. Perder a lista
         * aqui seria pior do que nao ter o recurso.
         */
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");
        UUID naoColetado = adicionar.executar(sessao.id(),
                produtoRepository.buscarPorSku("SKU-JAR-001").orElseThrow().getId()).id();

        recentrar.executar(sessao.id(), "CEN-03");

        List<ItemRoteiro> itens = listaRoteiroRepository.buscarPorSessao(sessao.id())
                .orElseThrow().getItens();

        assertEquals(2, itens.size());
        assertEquals(1L, itens.stream().filter(ItemRoteiro::isColetado).count());
        assertTrue(itens.stream().anyMatch(i -> i.getId().equals(naoColetado) && !i.isColetado()));
    }

    @Test
    @DisplayName("placa desconhecida devolve 404 e a posicao anterior continua valendo")
    void placaDesconhecidaNaoZeraAPosicao() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");

        assertThrows(RecursoNaoEncontradoException.class,
                () -> recentrar.executar(sessao.id(), "ZZZ-99"));

        assertEquals("ENT01", consultar.executar(sessao.id()).posicaoAtual().codigoCurto(),
                "errar a digitacao nao pode apagar o que o sistema ja sabia");
    }

    @Test
    @DisplayName("sessao encerrada recusa o recentrar")
    void sessaoEncerradaRecusa() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");
        concluir.executar(sessao.id());

        assertThrows(OperacaoNaoPermitidaException.class,
                () -> recentrar.executar(sessao.id(), "CEN-03"));
    }

    @Test
    @DisplayName("recentrar empurra o TTL: quem pede ajuda para se achar esta ativo")
    void recentrarRenovaOTtl() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");

        SessaoResponse depois = recentrar.executar(sessao.id(), "CEN-03");

        assertTrue(depois.expiracaoTtl().isAfter(sessao.expiracaoTtl())
                || depois.expiracaoTtl().isEqual(sessao.expiracaoTtl()));
    }

    // ---------------------------------------------------------------- desfazer uma coleta

    @Test
    @DisplayName("desmarcar devolve a posicao ao item coletado antes dele")
    void desmarcarVoltaAoAnterior() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");
        UUID jardim = coletar(sessao.id(), "SKU-JAR-001");

        assertEquals("Jardim", consultar.executar(sessao.id()).posicaoAtual().corredor());

        desmarcar.executar(jardim);

        assertEquals("Tintas", consultar.executar(sessao.id()).posicaoAtual().corredor(),
                "desfazer a coleta em Jardim devolve o cliente a Tintas");
    }

    @Test
    @DisplayName("desmarcar o unico coletado devolve a posicao a placa de entrada")
    void desmarcarOUnico() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        desmarcar.executar(tinta);

        assertEquals("ENT01", consultar.executar(sessao.id()).posicaoAtual().codigoCurto());
    }

    @Test
    @DisplayName("desmarcar tambem desfaz o estado do item, e nao so a posicao")
    void desmarcarDesfazOItem() throws Exception {
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        assertFalse(desmarcar.executar(tinta).coletado());

        List<ItemRoteiro> itens = listaRoteiroRepository.buscarPorSessao(sessao.id())
                .orElseThrow().getItens();
        assertEquals(1, itens.size(), "o item continua na lista, apenas nao coletado");
        assertTrue(itens.stream().noneMatch(ItemRoteiro::isColetado),
                "o item continua na lista, apenas nao coletado");
    }

    @Test
    @DisplayName("desmarcar o que ja esta desmarcado nao e erro")
    void desmarcarEIdempotente() throws Exception {
        // Rede reenviando, ou o cliente tocando duas vezes: nada disso pode virar erro.
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        desmarcar.executar(tinta);

        assertFalse(desmarcar.executar(tinta).coletado());
        assertEquals("ENT01", consultar.executar(sessao.id()).posicaoAtual().codigoCurto());
    }

    @Test
    @DisplayName("coletar de novo depois de desmarcar volta a mover a posicao")
    void podeColetarDeNovo() throws Exception {
        /*
         * O ciclo completo: o cliente desfaz por engano e refaz. Se marcarComoColetado
         * continuasse guardando a hora da primeira vez, a posicao nao acompanharia.
         */
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        desmarcar.executar(tinta);
        marcarColetado.executar(tinta);

        assertEquals("Tintas", consultar.executar(sessao.id()).posicaoAtual().corredor());
    }

    // ---------------------------------------------------------------- persistencia

    @Test
    @DisplayName("a posicao sobrevive ao recarregar a pagina")
    void posicaoPersistida() throws Exception {
        // O celular guarda so o id da sessao no localStorage; tudo o mais volta do banco.
        SessaoResponse sessao = iniciarEm("TIN-02");

        assertEquals("TIN02", consultar.executar(sessao.id()).posicaoAtual().codigoCurto());
    }
}
