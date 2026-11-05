package br.com.jence.backend;

import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.ExpirarSessoesInativasUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
    private final SessaoDao sessaoDao = BancoDeTeste.sessoes();
    private final ProdutoDao produtoDao = BancoDeTeste.produtos();


    private final List<UUID> criadas = new ArrayList<>();

    @AfterEach
    void limpar() throws Exception {
        for (UUID id : criadas) {
            BancoDeTeste.apagarSessao(id);
        }
        criadas.clear();
    }

    /** Sessao real, com o TTL empurrado para o passado como se o cliente tivesse sumido. */
    private UUID sessaoAbandonadaHaUmaHora(boolean comItem) throws Exception {
        UUID sessaoId = inicializar.executar(null).getId();
        criadas.add(sessaoId);

        if (comItem) {
            Produto produto = produtoDao.buscarPorSku("SKU-TIN-001").orElseThrow();
            adicionar.executar(sessaoId, produto.getId());
        }

        // Depois de adicionar, porque adicionar renova o TTL (D-24).
        LocalDateTime passado = LocalDateTime.now().minusHours(1);
        sessaoDao.salvar(Sessao.reconstituir(
                sessaoId, StatusSessao.ACTIVE, passado.minusHours(1), passado, null, null));

        return sessaoId;
    }

    private StatusSessao statusNoBanco(UUID sessaoId) throws Exception {
        return sessaoDao.buscarPorId(sessaoId).orElseThrow().getStatus();
    }

    @Test
    void classificaCadaSessaoVencidaESalvaNoBanco() throws Exception {
        UUID comCarrinho = sessaoAbandonadaHaUmaHora(true);
        UUID semCarrinho = sessaoAbandonadaHaUmaHora(false);

        var resultado = expirar.executar();

        System.out.println(">>> varredura: " + resultado);
        System.out.println(">>> com carrinho -> " + statusNoBanco(comCarrinho));
        System.out.println(">>> sem carrinho -> " + statusNoBanco(semCarrinho));

        assertEquals(StatusSessao.ABANDONED, statusNoBanco(comCarrinho),
                "lista montada e uma venda que quase aconteceu");

        assertEquals(StatusSessao.EXPIRED, statusNoBanco(semCarrinho),
                "sem itens, nada estava em jogo");
    }

    @Test
    void sessaoAindaValidaNaoEVarrida() throws Exception {
        UUID ativa = inicializar.executar(null).getId();
        criadas.add(ativa);

        expirar.executar();

        assertEquals(StatusSessao.ACTIVE, statusNoBanco(ativa),
                "o TTL dela ainda nao venceu");
    }

    @Test
    void segundaVarreduraNaoEncontraOQueAPrimeiraJaTratou() throws Exception {
        sessaoAbandonadaHaUmaHora(true);
        sessaoAbandonadaHaUmaHora(false);

        expirar.executar();
        var segunda = expirar.executar();

        assertEquals(0, segunda.total(),
                "a consulta filtra por ACTIVE, entao a varredura e naturalmente idempotente");
    }
}
