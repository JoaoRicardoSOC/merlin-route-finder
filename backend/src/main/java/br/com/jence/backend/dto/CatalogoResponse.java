package br.com.jence.backend.dto;

import br.com.jence.backend.model.FacetaDeProdutos;
import br.com.jence.backend.model.Pagina;
import br.com.jence.backend.model.Produto;

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
public class CatalogoResponse {

    private List<ProdutoResponse> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private List<FacetaResponse> facetas;

    public CatalogoResponse() {
    }

    public List<ProdutoResponse> getContent() {
        return content;
    }

    public void setContent(List<ProdutoResponse> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<FacetaResponse> getFacetas() {
        return facetas;
    }

    public void setFacetas(List<FacetaResponse> facetas) {
        this.facetas = facetas;
    }

    public static CatalogoResponse de(Pagina<Produto> pagina, List<FacetaDeProdutos> facetas) {
        CatalogoResponse resposta = new CatalogoResponse();
        resposta.setContent(pagina.conteudo().stream().map(ProdutoResponse::de).toList());
        resposta.setPage(pagina.pagina());
        resposta.setSize(pagina.tamanho());
        resposta.setTotalElements(pagina.totalElementos());
        resposta.setTotalPages(pagina.totalPaginas());
        resposta.setFacetas(facetas.stream().map(FacetaResponse::de).toList());
        return resposta;
    }
}
