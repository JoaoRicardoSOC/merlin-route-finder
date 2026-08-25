package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.repository.FacetaDeProdutos;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.entity.Produto;

import java.util.List;

/**
 * A resposta da tela de catalogo: a pagina de produtos e os filtros disponiveis para ela.
 * <p>
 * Os campos da pagina sao repetidos em vez de aninhados sob um objeto: assim o consumidor le
 * {@code content} no mesmo lugar de sempre, e as facetas entram como um campo a mais em vez de
 * mudarem a forma de tudo.
 * <p>
 * <b>Os dois vem juntos porque a tela precisa dos dois ao mesmo tempo.</b> Um endpoint
 * separado de facetas obrigaria o celular a repetir os filtros na URL e a fazer duas viagens
 * a cada toque - dentro de uma loja, com sinal ruim.
 */
public record CatalogoResponse(
        List<ProdutoResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        List<FacetaResponse> facetas
) {
    public static CatalogoResponse de(Pagina<Produto> pagina, List<FacetaDeProdutos> facetas) {
        return new CatalogoResponse(
                pagina.conteudo().stream().map(ProdutoResponse::de).toList(),
                pagina.pagina(),
                pagina.tamanho(),
                pagina.totalElementos(),
                pagina.totalPaginas(),
                facetas.stream().map(FacetaResponse::de).toList());
    }
}
