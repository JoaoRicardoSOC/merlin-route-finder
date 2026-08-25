package br.com.jence.backend;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConsultarSessaoUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.domain.entity.TipoPonto;
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

    @Test
    @DisplayName("a posicao sobrevive ao recarregar a pagina")
    void posicaoPersistida() {
        // O celular guarda so o id da sessao no localStorage; tudo o mais volta do banco.
        SessaoResponse sessao = iniciarEm("TIN-02");

        assertThat(consultar.executar(sessao.id()).posicaoAtual().codigoCurto())
                .isEqualTo("TIN02");
    }
}
