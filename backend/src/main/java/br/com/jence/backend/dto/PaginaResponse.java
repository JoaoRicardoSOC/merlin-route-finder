package br.com.jence.backend.dto;

import br.com.jence.backend.model.Pagina;

import java.util.List;
import java.util.function.Function;

/**
 * Espelha o schema {@code ProdutoPage} do contrato OpenAPI. Generico para ser reaproveitado
 * por outras listagens paginadas que venham a existir.
 */
public class PaginaResponse<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;

    public PaginaResponse() {
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
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

    public static <D, R> PaginaResponse<R> de(Pagina<D> pagina, Function<D, R> conversor) {
        PaginaResponse<R> resposta = new PaginaResponse<>();
        resposta.setContent(pagina.conteudo().stream().map(conversor).toList());
        resposta.setPage(pagina.pagina());
        resposta.setSize(pagina.tamanho());
        resposta.setTotalElements(pagina.totalElementos());
        resposta.setTotalPages(pagina.totalPaginas());
        return resposta;
    }
}
