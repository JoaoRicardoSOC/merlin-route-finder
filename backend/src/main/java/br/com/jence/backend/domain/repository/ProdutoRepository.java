package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.Produto;

import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository {

    Optional<Produto> buscarPorId(UUID id);

    Optional<Produto> buscarPorSku(String sku);

    Pagina<Produto> buscarPaginado(int pagina, int tamanho);

    /** Busca por nome, tolerante a busca parcial e a pequenos erros de digitacao. */
    Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho);

    Produto salvar(Produto produto);
}
