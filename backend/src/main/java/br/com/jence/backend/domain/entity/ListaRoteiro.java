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

    public List<ItemRoteiro> getItensOrdenados() {
        return itens.stream()
                .sorted(Comparator.comparing(ItemRoteiro::getOrdemCaminho,
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
