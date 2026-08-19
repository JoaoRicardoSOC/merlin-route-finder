package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.PaginaResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UC-002: busca e filtragem de produtos no Totem.
 */
@Service
@RequiredArgsConstructor
public class BuscarProdutosUseCase {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    private final ProdutoRepository produtoRepository;

    public PaginaResponse<ProdutoResponse> executar(String termo, Integer pagina, Integer tamanho) {
        int paginaSegura = Math.max(0, pagina == null ? 0 : pagina);
        int tamanhoSeguro = normalizarTamanho(tamanho);

        // Termo em branco significa navegar o catalogo, nao buscar pela string vazia.
        Pagina<Produto> resultado = (termo == null || termo.isBlank())
                ? produtoRepository.buscarPaginado(paginaSegura, tamanhoSeguro)
                : produtoRepository.buscarPorTermo(termo.trim(), paginaSegura, tamanhoSeguro);

        return PaginaResponse.de(resultado, ProdutoResponse::de);
    }

    /* Limita o tamanho para que um size absurdo na URL nao carregue o catalogo inteiro em memoria. */
    private int normalizarTamanho(Integer tamanho) {
        if (tamanho == null || tamanho < 1) {
            return TAMANHO_PADRAO;
        }
        return Math.min(tamanho, TAMANHO_MAXIMO);
    }
}
