package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Getter
public class ListaRoteiro {

    public static final Duration TTL_TOKEN_HANDOFF = Duration.ofMinutes(5);

    private final UUID id;
    private final UUID sessaoId;
    private final List<ItemRoteiro> itens;
    private String handoffToken;
    private LocalDateTime tokenExpiracao;

    private ListaRoteiro(UUID id, UUID sessaoId, List<ItemRoteiro> itens,
                         String handoffToken, LocalDateTime tokenExpiracao) {
        this.id = id;
        this.sessaoId = sessaoId;
        this.itens = itens;
        this.handoffToken = handoffToken;
        this.tokenExpiracao = tokenExpiracao;
    }

    public static ListaRoteiro criarPara(UUID id, UUID sessaoId) {
        return new ListaRoteiro(id, sessaoId, new ArrayList<>(), null, null);
    }

    public static ListaRoteiro reconstituir(UUID id, UUID sessaoId, List<ItemRoteiro> itens,
                                            String handoffToken, LocalDateTime tokenExpiracao) {
        return new ListaRoteiro(id, sessaoId, new ArrayList<>(itens), handoffToken, tokenExpiracao);
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

    public void registrarTokenHandoff(String tokenAssinado) {
        this.handoffToken = tokenAssinado;
        this.tokenExpiracao = LocalDateTime.now().plus(TTL_TOKEN_HANDOFF);
    }

    public boolean isTokenValido() {
        return isTokenValido(LocalDateTime.now());
    }

    public boolean isTokenValido(LocalDateTime referencia) {
        return handoffToken != null && tokenExpiracao != null && referencia.isBefore(tokenExpiracao);
    }

    public void invalidarToken() {
        this.handoffToken = null;
        this.tokenExpiracao = null;
    }

    private Optional<ItemRoteiro> buscarItemPorProduto(UUID produtoId) {
        return itens.stream()
                .filter(item -> item.getProduto().getId().equals(produtoId))
                .findFirst();
    }
}
