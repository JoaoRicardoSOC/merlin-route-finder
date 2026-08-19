package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.repository.Pagina;

import java.util.List;
import java.util.function.Function;

/**
 * Espelha o schema {@code ProdutoPage} do contrato OpenAPI. Generico para ser reaproveitado
 * por outras listagens paginadas que venham a existir.
 */
public record PaginaResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static <D, R> PaginaResponse<R> de(Pagina<D> pagina, Function<D, R> conversor) {
        return new PaginaResponse<>(
                pagina.conteudo().stream().map(conversor).toList(),
                pagina.pagina(),
                pagina.tamanho(),
                pagina.totalElementos(),
                pagina.totalPaginas()
        );
    }
}
