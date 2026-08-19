package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.util.UUID;

@Getter
public class ItemRoteiro {

    private final UUID id;
    private final Produto produto;
    private Integer ordemCaminho;
    private boolean coletado;

    public ItemRoteiro(UUID id, Produto produto) {
        this.id = id;
        this.produto = produto;
        this.ordemCaminho = null;
        this.coletado = false;
    }

    private ItemRoteiro(UUID id, Produto produto, Integer ordemCaminho, boolean coletado) {
        this.id = id;
        this.produto = produto;
        this.ordemCaminho = ordemCaminho;
        this.coletado = coletado;
    }

    public static ItemRoteiro reconstituir(UUID id, Produto produto, Integer ordemCaminho, boolean coletado) {
        return new ItemRoteiro(id, produto, ordemCaminho, coletado);
    }

    public void marcarComoColetado() {
        this.coletado = true;
    }

    public void definirOrdem(Integer ordem) {
        this.ordemCaminho = ordem;
    }
}
