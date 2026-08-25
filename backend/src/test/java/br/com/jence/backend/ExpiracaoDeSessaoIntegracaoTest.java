package br.com.jence.backend;

import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ExpirarSessoesInativasUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.StatusSessao;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A varredura de TTL contra o Oracle real: sessoes vencidas de verdade, gravadas de verdade.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class ExpiracaoDeSessaoIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired ExpirarSessoesInativasUseCase expirar;
    @Autowired SessaoRepository sessaoRepository;
    @Autowired ProdutoRepository produtoRepository;

    @Autowired SessaoJpaRepository sessaoJpa;
    @Autowired ListaRoteiroJpaRepository listaJpa;

    private final List<UUID> criadas = new ArrayList<>();

    @AfterEach
    void limpar() {
        criadas.forEach(id -> {
            listaJpa.findBySessaoId(id).ifPresent(listaJpa::delete);
            sessaoJpa.deleteById(id);
        });
        criadas.clear();
    }

    /** Sessao real, com o TTL empurrado para o passado como se o cliente tivesse sumido. */
    private UUID sessaoAbandonadaHaUmaHora(boolean comItem) {
        UUID sessaoId = inicializar.executar(null).id();
        criadas.add(sessaoId);

        if (comItem) {
            Produto produto = produtoRepository.buscarPorSku("SKU-TIN-001").orElseThrow();
            adicionar.executar(sessaoId, produto.getId());
        }

        // Depois de adicionar, porque adicionar renova o TTL (D-24).
        LocalDateTime passado = LocalDateTime.now().minusHours(1);
        sessaoRepository.salvar(Sessao.reconstituir(
                sessaoId, StatusSessao.ACTIVE, passado.minusHours(1), passado, null, null));

        return sessaoId;
    }

    private StatusSessao statusNoBanco(UUID sessaoId) {
        return sessaoRepository.buscarPorId(sessaoId).orElseThrow().getStatus();
    }

    @Test
    void classificaCadaSessaoVencidaESalvaNoBanco() {
        UUID comCarrinho = sessaoAbandonadaHaUmaHora(true);
        UUID semCarrinho = sessaoAbandonadaHaUmaHora(false);

        var resultado = expirar.executar();

        System.out.println(">>> varredura: " + resultado);
        System.out.println(">>> com carrinho -> " + statusNoBanco(comCarrinho));
        System.out.println(">>> sem carrinho -> " + statusNoBanco(semCarrinho));

        assertThat(statusNoBanco(comCarrinho))
                .as("lista montada e uma venda que quase aconteceu")
                .isEqualTo(StatusSessao.ABANDONED);

        assertThat(statusNoBanco(semCarrinho))
                .as("sem itens, nada estava em jogo")
                .isEqualTo(StatusSessao.EXPIRED);
    }

    @Test
    void sessaoAindaValidaNaoEVarrida() {
        UUID ativa = inicializar.executar(null).id();
        criadas.add(ativa);

        expirar.executar();

        assertThat(statusNoBanco(ativa))
                .as("o TTL dela ainda nao venceu")
                .isEqualTo(StatusSessao.ACTIVE);
    }

    @Test
    void segundaVarreduraNaoEncontraOQueAPrimeiraJaTratou() {
        sessaoAbandonadaHaUmaHora(true);
        sessaoAbandonadaHaUmaHora(false);

        expirar.executar();
        var segunda = expirar.executar();

        assertThat(segunda.total())
                .as("a consulta filtra por ACTIVE, entao a varredura e naturalmente idempotente")
                .isZero();
    }
}
