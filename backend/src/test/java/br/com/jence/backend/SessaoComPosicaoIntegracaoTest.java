package br.com.jence.backend;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConsultarSessaoUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.ConcluirRotaUseCase;
import br.com.jence.backend.application.usecase.DesmarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.RecentrarSessaoUseCase;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    private UUID coletar(UUID sessaoId, String sku) {
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

        assertThat(sessao.posicaoAtual()).isNotNull();
        assertThat(sessao.posicaoAtual().codigoCurto()).isEqualTo("ENT01");
        assertThat(sessao.posicaoAtual().tipo()).isEqualTo(TipoPonto.QR_CODE);
    }

    @Test
    @DisplayName("o plano B chega pelo mesmo caminho: codigo digitado em qualquer grafia")
    void codigoDigitado() {
        // O cliente que nao conseguiu escanear digita o que le na placa, do jeito que le.
        assertThat(iniciarEm("ent 01").posicaoAtual().codigoCurto()).isEqualTo("ENT01");
        assertThat(iniciarEm("Tin-02").posicaoAtual().codigoCurto()).isEqualTo("TIN02");
    }

    @Test
    @DisplayName("codigo desconhecido nao recusa a sessao: ela nasce sem posicao e funciona")
    void codigoDesconhecidoNaoBarraAEntrada() {
        /*
         * Placa velha, loja remanejada ou erro de digitacao. Recusar aqui seria barrar a
         * entrada por causa de um adesivo - o cliente ficaria sem sistema nenhum.
         */
        SessaoResponse sessao = iniciarEm("ZZZ-99");

        assertThat(sessao.posicaoAtual()).isNull();
        assertThat(sessao.id()).isNotNull();

        UUID produtoId = produtoRepository.buscarPorSku("SKU-TIN-001").orElseThrow().getId();
        assertThat(adicionar.executar(sessao.id(), produtoId)).isNotNull();
        assertThat(listaRoteiroRepository.buscarPorSessao(sessao.id())).isPresent();
    }

    @Test
    @DisplayName("sem codigo algum a sessao tambem nasce, so que sem posicao")
    void semCodigo() {
        assertThat(iniciarEm(null).posicaoAtual()).isNull();
    }

    // ---------------------------------------------------------------- a posicao acompanha a coleta

    @Test
    @DisplayName("coletar um item move a posicao para a prateleira dele")
    void coletaMoveAPosicao() {
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");

        assertThat(consultar.executar(sessao.id()).posicaoAtual().corredor())
                .as("depois de pegar a tinta, o cliente esta em Tintas, nao na entrada")
                .isEqualTo("Tintas");
    }

    @Test
    @DisplayName("com varios coletados, a posicao e a do ultimo")
    void ultimoColetadoVence() {
        SessaoResponse sessao = iniciarEm("ENT-01");

        coletar(sessao.id(), "SKU-TIN-001");
        coletar(sessao.id(), "SKU-JAR-001");

        String corredor = consultar.executar(sessao.id()).posicaoAtual().corredor();

        System.out.println(">>> posicao apos duas coletas: " + corredor);
        assertThat(corredor).isEqualTo("Jardim");
    }

    // ---------------------------------------------------------------- recentrar

    @Test
    @DisplayName("recentrar move a posicao e vence o ultimo item coletado")
    void recentrarVenceAColeta() {
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");

        SessaoResponse depois = recentrar.executar(sessao.id(), "CEN-03");

        assertThat(depois.posicaoAtual().codigoCurto())
                .as("quem leu uma placa agora nao esta mais na prateleira de antes")
                .isEqualTo("CEN03");
        assertThat(consultar.executar(sessao.id()).posicaoAtual().codigoCurto()).isEqualTo("CEN03");
    }

    @Test
    @DisplayName("recentrar nao mexe na lista nem no que ja foi coletado")
    void recentrarPreservaALista() {
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

        assertThat(itens).hasSize(2);
        assertThat(itens).filteredOn(ItemRoteiro::isColetado).hasSize(1);
        assertThat(itens).anyMatch(i -> i.getId().equals(naoColetado) && !i.isColetado());
    }

    @Test
    @DisplayName("placa desconhecida devolve 404 e a posicao anterior continua valendo")
    void placaDesconhecidaNaoZeraAPosicao() {
        SessaoResponse sessao = iniciarEm("ENT-01");

        assertThatThrownBy(() -> recentrar.executar(sessao.id(), "ZZZ-99"))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(consultar.executar(sessao.id()).posicaoAtual().codigoCurto())
                .as("errar a digitacao nao pode apagar o que o sistema ja sabia")
                .isEqualTo("ENT01");
    }

    @Test
    @DisplayName("sessao encerrada recusa o recentrar")
    void sessaoEncerradaRecusa() {
        SessaoResponse sessao = iniciarEm("ENT-01");
        concluir.executar(sessao.id());

        assertThatThrownBy(() -> recentrar.executar(sessao.id(), "CEN-03"))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    @DisplayName("recentrar empurra o TTL: quem pede ajuda para se achar esta ativo")
    void recentrarRenovaOTtl() {
        SessaoResponse sessao = iniciarEm("ENT-01");

        SessaoResponse depois = recentrar.executar(sessao.id(), "CEN-03");

        assertThat(depois.expiracaoTtl()).isAfterOrEqualTo(sessao.expiracaoTtl());
    }

    // ---------------------------------------------------------------- desfazer uma coleta

    @Test
    @DisplayName("desmarcar devolve a posicao ao item coletado antes dele")
    void desmarcarVoltaAoAnterior() {
        SessaoResponse sessao = iniciarEm("ENT-01");
        coletar(sessao.id(), "SKU-TIN-001");
        UUID jardim = coletar(sessao.id(), "SKU-JAR-001");

        assertThat(consultar.executar(sessao.id()).posicaoAtual().corredor()).isEqualTo("Jardim");

        desmarcar.executar(jardim);

        assertThat(consultar.executar(sessao.id()).posicaoAtual().corredor())
                .as("desfazer a coleta em Jardim devolve o cliente a Tintas")
                .isEqualTo("Tintas");
    }

    @Test
    @DisplayName("desmarcar o unico coletado devolve a posicao a placa de entrada")
    void desmarcarOUnico() {
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        desmarcar.executar(tinta);

        assertThat(consultar.executar(sessao.id()).posicaoAtual().codigoCurto())
                .isEqualTo("ENT01");
    }

    @Test
    @DisplayName("desmarcar tambem desfaz o estado do item, e nao so a posicao")
    void desmarcarDesfazOItem() {
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        assertThat(desmarcar.executar(tinta).coletado()).isFalse();

        assertThat(listaRoteiroRepository.buscarPorSessao(sessao.id()).orElseThrow().getItens())
                .as("o item continua na lista, apenas nao coletado")
                .hasSize(1)
                .noneMatch(ItemRoteiro::isColetado);
    }

    @Test
    @DisplayName("desmarcar o que ja esta desmarcado nao e erro")
    void desmarcarEIdempotente() {
        // Rede reenviando, ou o cliente tocando duas vezes: nada disso pode virar erro.
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        desmarcar.executar(tinta);

        assertThat(desmarcar.executar(tinta).coletado()).isFalse();
        assertThat(consultar.executar(sessao.id()).posicaoAtual().codigoCurto()).isEqualTo("ENT01");
    }

    @Test
    @DisplayName("coletar de novo depois de desmarcar volta a mover a posicao")
    void podeColetarDeNovo() {
        /*
         * O ciclo completo: o cliente desfaz por engano e refaz. Se marcarComoColetado
         * continuasse guardando a hora da primeira vez, a posicao nao acompanharia.
         */
        SessaoResponse sessao = iniciarEm("ENT-01");
        UUID tinta = coletar(sessao.id(), "SKU-TIN-001");

        desmarcar.executar(tinta);
        marcarColetado.executar(tinta);

        assertThat(consultar.executar(sessao.id()).posicaoAtual().corredor()).isEqualTo("Tintas");
    }

    // ---------------------------------------------------------------- persistencia

    @Test
    @DisplayName("a posicao sobrevive ao recarregar a pagina")
    void posicaoPersistida() {
        // O celular guarda so o id da sessao no localStorage; tudo o mais volta do banco.
        SessaoResponse sessao = iniciarEm("TIN-02");

        assertThat(consultar.executar(sessao.id()).posicaoAtual().codigoCurto())
                .isEqualTo("TIN02");
    }
}
