package br.com.jence.backend.domain.repository;

import java.util.List;

/**
 * Resultado paginado expresso em termos de dominio.
 * <p>
 * Existe para que as interfaces de repositorio nao precisem do {@code Page} do Spring Data:
 * o pacote {@code domain} permanece livre de qualquer dependencia de framework, e a traducao
 * acontece apenas nos adaptadores de persistencia.
 */
public record Pagina<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas
) {
    public static <T> Pagina<T> vazia(int pagina, int tamanho) {
        return new Pagina<>(List.of(), pagina, tamanho, 0L, 0);
    }
}
