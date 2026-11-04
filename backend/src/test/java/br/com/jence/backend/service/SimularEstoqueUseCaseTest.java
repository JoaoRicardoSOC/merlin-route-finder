package br.com.jence.backend.service;

import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dto.ProdutoResponse;
import br.com.jence.backend.factory.ConexaoFalsa;
import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimularEstoqueUseCaseTest {

    /**
     * Catalogo em memoria no lugar do banco. Sobrescreve so o que o caso de uso usa e anota
     * cada produto salvo, para o teste conferir o que foi gravado.
     */
    private static class ProdutoDaoFalso extends ProdutoDao {

        final Map<UUID, Produto> catalogo = new HashMap<>();
        final List<Produto> salvos = new ArrayList<>();

        @Override
        public Optional<Produto> buscarPorId(UUID id) {
            return Optional.ofNullable(catalogo.get(id));
        }

        @Override
        public Produto salvar(Produto produto) {
            salvos.add(produto);
            catalogo.put(produto.getId(), produto);
            return produto;
        }
    }

    private static final PontoMapa TINTAS =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

    private ProdutoDaoFalso produtoDao;
    private SimularEstoqueUseCase useCase;

    private UUID produtoId;
    private Produto lixa;

    @BeforeEach
    void preparar() {
        ConexaoFalsa.usarNaFabrica();
        produtoDao = new ProdutoDaoFalso();
        useCase = new SimularEstoqueUseCase(produtoDao);

        produtoId = UUID.randomUUID();
        lixa = new Produto(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120",
                new BigDecimal("3.50"), 40, TINTAS);
    }

    @AfterEach
    void devolverAFabricaAoBanco() {
        ConnectionFactory.usarBanco();
    }

    private void comProdutoNoCatalogo() {
        produtoDao.catalogo.put(produtoId, lixa);
    }

    /** O unico produto salvo; falha se o caso de uso salvou nenhum ou mais de um. */
    private Produto produtoSalvo() {
        assertEquals(1, produtoDao.salvos.size());
        return produtoDao.salvos.getFirst();
    }

    @Test
    @DisplayName("zerar o saldo dispara o cenario de ruptura")
    void zerarSaldo() throws Exception {
        comProdutoNoCatalogo();

        ProdutoResponse resposta = useCase.executar(produtoId, 0);

        assertEquals(0, resposta.saldoEstoque());
        assertFalse(produtoSalvo().temDisponibilidade());
    }

    @Test
    @DisplayName("restaurar o saldo desfaz o cenario, para a demo poder ser repetida")
    void restaurarSaldo() throws Exception {
        lixa = new Produto(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120",
                new BigDecimal("3.50"), 0, TINTAS);
        comProdutoNoCatalogo();

        ProdutoResponse resposta = useCase.executar(produtoId, 40);

        assertEquals(40, resposta.saldoEstoque());
        assertTrue(produtoSalvo().temDisponibilidade());
    }

    @Test
    @DisplayName("so o saldo muda: nome, preco, sku e posicao ficam intactos")
    void naoAlteraOResto() throws Exception {
        comProdutoNoCatalogo();

        useCase.executar(produtoId, 7);

        Produto salvo = produtoSalvo();
        assertEquals(produtoId, salvo.getId());
        assertEquals("SKU-TIN-003", salvo.getSku());
        assertEquals("Lixa para Parede Grao 120", salvo.getNome());
        assertEquals(0, salvo.getPreco().compareTo(new BigDecimal("3.50")));
        assertSame(TINTAS, salvo.getPontoMapa());
    }

    @Test
    @DisplayName("produto inexistente devolve 404 e nao grava nada")
    void produtoInexistente() {
        assertThrows(RecursoNaoEncontradoException.class, () -> useCase.executar(produtoId, 0));

        assertTrue(produtoDao.salvos.isEmpty());
    }

    @Test
    @DisplayName("saldo negativo e barrado pela propria entidade, alem da validacao do contrato")
    void saldoNegativoNaEntidade() {
        // O 400 vem do @Min no request; esta e a segunda linha de defesa, para o caso de
        // alguem chamar o caso de uso por outro caminho no futuro.
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> lixa.comSaldoEstoque(-1));
        assertTrue(e.getMessage().contains("nao pode ser negativo"));
    }
}
