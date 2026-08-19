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

    public void marcarComoColetado() {
        this.coletado = true;
    }

    public void definirOrdem(Integer ordem) {
        this.ordemCaminho = ordem;
    }
}
