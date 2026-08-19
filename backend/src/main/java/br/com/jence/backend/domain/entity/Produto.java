package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class Produto {

    private final UUID id;
    private final String sku;
    private final String nome;
    private final BigDecimal preco;
    private final int saldoEstoque;
    private final PontoMapa pontoMapa;

    public Produto(UUID id, String sku, String nome, BigDecimal preco, int saldoEstoque, PontoMapa pontoMapa) {
        this.id = id;
        this.sku = sku;
        this.nome = nome;
        this.preco = preco;
        this.saldoEstoque = saldoEstoque;
        this.pontoMapa = pontoMapa;
    }

    public boolean temDisponibilidade() {
        return saldoEstoque > 0;
    }
}
