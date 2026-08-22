package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SimularEstoqueUseCaseTest {

    @Mock ProdutoRepository produtoRepository;
    @InjectMocks SimularEstoqueUseCase useCase;

    private static final PontoMapa TINTAS =
            new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

    private UUID produtoId;
    private Produto lixa;

    @BeforeEach
    void preparar() {
        produtoId = UUID.randomUUID();
        lixa = new Produto(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120",
                new BigDecimal("3.50"), 40, TINTAS);
    }

    private void comProdutoNoCatalogo() {
        when(produtoRepository.buscarPorId(produtoId)).thenReturn(Optional.of(lixa));
        when(produtoRepository.salvar(any())).thenAnswer(i -> i.getArgument(0));
    }

    private Produto produtoSalvo() {
        ArgumentCaptor<Produto> captor = ArgumentCaptor.forClass(Produto.class);
        verify(produtoRepository).salvar(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("zerar o saldo dispara o cenario de ruptura")
    void zerarSaldo() {
        comProdutoNoCatalogo();

        ProdutoResponse resposta = useCase.executar(produtoId, 0);

        assertThat(resposta.saldoEstoque()).isZero();
        assertThat(produtoSalvo().temDisponibilidade()).isFalse();
    }

    @Test
    @DisplayName("restaurar o saldo desfaz o cenario, para a demo poder ser repetida")
    void restaurarSaldo() {
        lixa = new Produto(produtoId, "SKU-TIN-003", "Lixa para Parede Grao 120",
                new BigDecimal("3.50"), 0, TINTAS);
        comProdutoNoCatalogo();

        ProdutoResponse resposta = useCase.executar(produtoId, 40);

        assertThat(resposta.saldoEstoque()).isEqualTo(40);
        assertThat(produtoSalvo().temDisponibilidade()).isTrue();
    }

    @Test
    @DisplayName("so o saldo muda: nome, preco, sku e posicao ficam intactos")
    void naoAlteraOResto() {
        comProdutoNoCatalogo();

        useCase.executar(produtoId, 7);

        Produto salvo = produtoSalvo();
        assertThat(salvo.getId()).isEqualTo(produtoId);
        assertThat(salvo.getSku()).isEqualTo("SKU-TIN-003");
        assertThat(salvo.getNome()).isEqualTo("Lixa para Parede Grao 120");
        assertThat(salvo.getPreco()).isEqualByComparingTo("3.50");
        assertThat(salvo.getPontoMapa()).isSameAs(TINTAS);
    }

    @Test
    @DisplayName("produto inexistente devolve 404 e nao grava nada")
    void produtoInexistente() {
        when(produtoRepository.buscarPorId(produtoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.executar(produtoId, 0))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        verify(produtoRepository, never()).salvar(any());
    }

    @Test
    @DisplayName("saldo negativo e barrado pela propria entidade, alem da validacao do contrato")
    void saldoNegativoNaEntidade() {
        // O 400 vem do @Min no request; esta e a segunda linha de defesa, para o caso de
        // alguem chamar o caso de uso por outro caminho no futuro.
        assertThatThrownBy(() -> lixa.comSaldoEstoque(-1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode ser negativo");
    }
}
