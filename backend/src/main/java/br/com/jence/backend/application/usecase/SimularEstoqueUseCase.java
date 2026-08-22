package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Ferramenta operacional de demonstracao: zera ou restaura o saldo de um produto sob demanda.
 * <p>
 * <b>Nao corresponde a nenhum caso de uso do cliente final</b> e nao existiria num sistema
 * real, onde o saldo viria do ERP. Existe porque o fluxo de ruptura (UC-013) e o momento mais
 * forte da apresentacao e precisa ser disparado de forma confiavel durante uma gravacao ou uma
 * demonstracao ao vivo - sem depender de alguem lembrar de deixar o banco no estado certo.
 * <p>
 * Ver D-40 sobre o que isso expoe e por que foi aceito.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SimularEstoqueUseCase {

    private final ProdutoRepository produtoRepository;

    @Transactional
    public ProdutoResponse executar(UUID produtoId, int novoSaldo) {
        Produto produto = produtoRepository.buscarPorId(produtoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoId));

        Produto atualizado = produtoRepository.salvar(produto.comSaldoEstoque(novoSaldo));

        /*
         * Em nivel de aviso, e nao de informacao, de proposito: e uma alteracao manual de
         * catalogo feita por fora de qualquer regra de negocio. Se aparecer num log de
         * producao sem ninguem ter pedido, alguem precisa reparar.
         */
        log.warn("Simulacao de estoque: '{}' passou de {} para {} unidades",
                produto.getNome(), produto.getSaldoEstoque(), atualizado.getSaldoEstoque());

        return ProdutoResponse.de(atualizado);
    }
}
