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

    /**
     * Devolve uma copia do produto com outro saldo em estoque.
     * <p>
     * Copia em vez de mutacao porque a entidade e imutavel por padrao (D-04). O ajuste de
     * saldo existe apenas para a ferramenta de simulacao usada em demonstracao (D-40): o
     * sistema nao movimenta estoque, e nenhum fluxo do cliente final chama este metodo.
     *
     * @throws IllegalArgumentException se o saldo for negativo
     */
    public Produto comSaldoEstoque(int novoSaldo) {
        if (novoSaldo < 0) {
            throw new IllegalArgumentException("Saldo em estoque nao pode ser negativo: " + novoSaldo);
        }
        return new Produto(id, sku, nome, preco, novoSaldo, pontoMapa);
    }
}
