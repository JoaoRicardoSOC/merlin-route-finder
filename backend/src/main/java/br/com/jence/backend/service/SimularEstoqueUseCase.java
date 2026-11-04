package br.com.jence.backend.service;

import br.com.jence.backend.dto.ProdutoResponse;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ProdutoRepository;
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
public class SimularEstoqueUseCase {

    private final ProdutoRepository produtoRepository;

    public SimularEstoqueUseCase(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

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
        System.out.println(String.format("AVISO: Simulacao de estoque: '%s' passou de %s para %s unidades",
                produto.getNome(), produto.getSaldoEstoque(), atualizado.getSaldoEstoque()));

        return ProdutoResponse.de(atualizado);
    }
}
