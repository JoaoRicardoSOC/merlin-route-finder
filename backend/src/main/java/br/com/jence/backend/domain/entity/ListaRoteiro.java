package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public class ListaRoteiro {

    private final UUID id;
    private final UUID sessaoId;
    private final List<ItemRoteiro> itens;

    private ListaRoteiro(UUID id, UUID sessaoId, List<ItemRoteiro> itens) {
        this.id = id;
        this.sessaoId = sessaoId;
        this.itens = itens;
    }

    public static ListaRoteiro criarPara(UUID id, UUID sessaoId) {
        return new ListaRoteiro(id, sessaoId, new ArrayList<>());
    }

    public static ListaRoteiro reconstituir(UUID id, UUID sessaoId, List<ItemRoteiro> itens) {
        return new ListaRoteiro(id, sessaoId, new ArrayList<>(itens));
    }

    public ItemRoteiro adicionarProduto(Produto produto) {
        return buscarItemPorProduto(produto.getId())
                .orElseGet(() -> {
                    ItemRoteiro novoItem = new ItemRoteiro(UUID.randomUUID(), produto);
                    itens.add(novoItem);
                    return novoItem;
                });
    }

    public boolean removerProduto(UUID itemId) {
        return itens.removeIf(item -> item.getId().equals(itemId));
    }

    public List<ItemRoteiro> getItens() {
        return List.copyOf(itens);
    }

    /**
     * Os itens na ordem em que a lista deve ser exibida: agrupados por secao e, dentro dela,
     * por nome.
     * <p>
     * Nao e uma rota - o cliente escolhe o caminho dele. E agrupamento de exibicao, que serve
     * a quem esta decidindo por onde passar: ver "tres itens em Tintas" junto ajuda, e ver os
     * mesmos tres espalhados pela lista, nao. A ordenacao tambem precisa ser estavel, porque
     * a colecao vem do banco sem ordem garantida e a lista mudaria de posicao a cada consulta.
     */
    public List<ItemRoteiro> getItensParaExibicao() {
        return itens.stream()
                .sorted(Comparator
                        .comparing((ItemRoteiro item) -> item.getProduto().getPontoMapa().getCorredor(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(item -> item.getProduto().getNome(),
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public boolean isVazia() {
        return itens.isEmpty();
    }


    private Optional<ItemRoteiro> buscarItemPorProduto(UUID produtoId) {
        return itens.stream()
                .filter(item -> item.getProduto().getId().equals(produtoId))
                .findFirst();
    }
}
