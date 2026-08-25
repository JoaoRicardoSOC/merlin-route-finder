package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
public class Produto {

    private final UUID id;
    private final String sku;
    private final String nome;

    /** O que o produto e e para que serve. Nulo enquanto ninguem escreveu. */
    private final String descricao;

    /** URL publica da foto. Nula enquanto o time nao coletou a do produto. */
    private final String imagemUrl;

    private final BigDecimal preco;
    private final int saldoEstoque;
    private final PontoMapa pontoMapa;

    /** Produto sem apresentacao: valido, e o estado de tudo que ainda nao foi enriquecido. */
    public Produto(UUID id, String sku, String nome, BigDecimal preco, int saldoEstoque, PontoMapa pontoMapa) {
        this(id, sku, nome, null, null, preco, saldoEstoque, pontoMapa);
    }

    public Produto(UUID id, String sku, String nome, String descricao, String imagemUrl,
                   BigDecimal preco, int saldoEstoque, PontoMapa pontoMapa) {
        this.id = id;
        this.sku = sku;
        this.nome = nome;
        this.descricao = descricao;
        this.imagemUrl = imagemUrl;
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
        return new Produto(id, sku, nome, descricao, imagemUrl, preco, novoSaldo, pontoMapa);
    }

    /**
     * Devolve uma copia com descricao e imagem preenchidas.
     * <p>
     * Existe para a carga inicial completar produtos que ja estavam no banco antes de estes
     * campos existirem: a carga e incremental e nunca reescreve um SKU que ja esta la, entao
     * sem isto os 29 produtos originais ficariam sem apresentacao para sempre. Ver D-59.
     */
    public Produto comApresentacao(String novaDescricao, String novaImagemUrl) {
        return new Produto(id, sku, nome, novaDescricao, novaImagemUrl, preco, saldoEstoque, pontoMapa);
    }
}
