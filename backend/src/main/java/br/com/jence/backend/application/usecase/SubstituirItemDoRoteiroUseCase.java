package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ListaRoteiroResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * O cliente aceita o substituto: o produto que faltou sai da lista e o novo entra, numa
 * chamada so.
 * <p>
 * <b>E o que fecha o ciclo da ruptura.</b> A promessa do recurso e converter uma prateleira
 * vazia em venda; sem isto, aceitar a sugestao exigiria do cliente duas acoes - adicionar um
 * produto e remover outro - em pe no corredor, com o celular na mao. Enquanto aceitar da
 * trabalho, a conversao nao acontece.
 * <p>
 * <b>O substituto entra NAO coletado</b>, e nao herda o estado do item que saiu: ele nem
 * sempre esta na mesma prateleira, e pode estar alguns metros adiante. Marca-lo como coletado
 * mentiria sobre onde o cliente esta ({@link br.com.jence.backend.domain.service.PosicaoDoCliente})
 * e o faria sair sem o produto.
 * <p>
 * <b>O registro da ruptura nao e tocado.</b> Ele e evidencia do que aconteceu na gondola, e
 * continua valendo tenha o cliente aceitado a troca ou nao - inclusive porque comparar as
 * duas coisas e o que diz a loja se as sugestoes estao boas.
 */
@Service
@RequiredArgsConstructor
public class SubstituirItemDoRoteiroUseCase {

    private final ListaRoteiroRepository listaRoteiroRepository;
    private final SessaoRepository sessaoRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public ListaRoteiroResponse executar(UUID itemId, UUID produtoSubstitutoId) {
        ListaRoteiro lista = listaRoteiroRepository.buscarPorItem(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        ItemRoteiro emFalta = lista.getItens().stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        Sessao sessao = sessaoRepository.buscarPorId(lista.getSessaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", lista.getSessaoId()));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)"
                            .formatted(sessao.getId(), sessao.getStatus()));
        }

        Produto substituto = produtoRepository.buscarPorId(produtoSubstitutoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Produto", produtoSubstitutoId));

        /*
         * Trocar um produto por ele mesmo apagaria o item silenciosamente: o adicionar
         * devolveria o item existente e o remover o apagaria em seguida. O cliente ficaria sem
         * o produto sem ter pedido isso.
         */
        if (emFalta.getProduto().getId().equals(produtoSubstitutoId)) {
            throw new OperacaoNaoPermitidaException(
                    "Produto %s nao pode substituir a si mesmo".formatted(produtoSubstitutoId));
        }

        // A entidade decide o que fazer com produto repetido (D-18): se o substituto ja estava
        // na lista, devolve o item existente em vez de criar outro.
        lista.adicionarProduto(substituto);
        lista.removerProduto(itemId);

        listaRoteiroRepository.salvar(lista);

        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        return ListaRoteiroResponse.de(lista);
    }
}
