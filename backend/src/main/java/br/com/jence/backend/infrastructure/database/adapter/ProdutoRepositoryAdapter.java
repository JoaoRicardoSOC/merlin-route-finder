package br.com.jence.backend.infrastructure.database.adapter;

import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import br.com.jence.backend.infrastructure.database.factory.ProdutoFactory;
import br.com.jence.backend.infrastructure.database.repository.ProdutoJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ProdutoRepositoryAdapter implements ProdutoRepository {

    private final ProdutoJpaRepository jpaRepository;
    private final ProdutoFactory produtoFactory;

    @Override
    @Transactional(readOnly = true)
    public Optional<Produto> buscarPorId(UUID id) {
        return jpaRepository.findById(id).map(produtoFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Produto> buscarPorSku(String sku) {
        return jpaRepository.findBySku(sku).map(produtoFactory::paraDominio);
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Produto> buscarPaginado(int pagina, int tamanho) {
        return converter(jpaRepository.findAll(PageRequest.of(pagina, tamanho)));
    }

    @Override
    @Transactional(readOnly = true)
    public Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho) {
        return converter(jpaRepository.buscarPorTermo(termo, PageRequest.of(pagina, tamanho)));
    }

    private Pagina<Produto> converter(Page<ProdutoEntity> page) {
        return new Pagina<>(
                page.getContent().stream().map(produtoFactory::paraDominio).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public Produto salvar(Produto produto) {
        ProdutoEntity salvo = jpaRepository.save(produtoFactory.paraPersistencia(produto));
        return produtoFactory.paraDominio(salvo);
    }
}
