package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ItemRoteiro {

    private final UUID id;
    private final Produto produto;
    private boolean coletado;

    public ItemRoteiro(UUID id, Produto produto) {
        this.id = id;
        this.produto = produto;
        this.coletado = false;
    }

    private ItemRoteiro(UUID id, Produto produto, boolean coletado) {
        this.id = id;
        this.produto = produto;
        this.coletado = coletado;
    }

    public static ItemRoteiro reconstituir(UUID id, Produto produto, boolean coletado) {
        return new ItemRoteiro(id, produto, coletado);
    }

    public void marcarComoColetado() {
        this.coletado = true;
    }
}
