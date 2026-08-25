package br.com.jence.backend.infrastructure.database.factory;

import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.infrastructure.database.entity.ProdutoEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProdutoFactory {

    private final PontoMapaFactory pontoMapaFactory;

    public Produto paraDominio(ProdutoEntity entity) {
        if (entity == null) {
            return null;
        }
        return new Produto(
                entity.getId(),
                entity.getSku(),
                entity.getNome(),
                entity.getDescricao(),
                entity.getImagemUrl(),
                entity.getPreco(),
                entity.getSaldoEstoque(),
                pontoMapaFactory.paraDominio(entity.getPontoMapa())
        );
    }

    public ProdutoEntity paraPersistencia(Produto produto) {
        if (produto == null) {
            return null;
        }
        return new ProdutoEntity(
                produto.getId(),
                produto.getSku(),
                produto.getNome(),
                produto.getDescricao(),
                produto.getImagemUrl(),
                produto.getPreco(),
                produto.getSaldoEstoque(),
                pontoMapaFactory.paraPersistencia(produto.getPontoMapa())
        );
    }
}
