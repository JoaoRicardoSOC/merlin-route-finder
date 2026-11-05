package br.com.jence.backend.dto;

import br.com.jence.backend.factory.MapperFactory;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Espelha o schema {@code Produto} do contrato OpenAPI.
 * <p>
 * Expoe apenas o {@code pontoMapaId}, nao o ponto de mapa completo: a versao com o ponto
 * aninhado e o {@code ProdutoDetalhado}, usado no endpoint de detalhamento (UC-003).
 * <p>
 * <b>O nome do corredor vem junto, e nao so o id do ponto.</b> Sem ele a listagem obrigaria a
 * tela a cruzar cada produto com {@code GET /mapa} para descobrir onde ele fica - e enquanto
 * isso nao acontecia, o catalogo inteiro exibia um texto generico no lugar do corredor. Saber
 * em que corredor o produto esta e o que separa este sistema de um e-commerce, entao ele viaja
 * na resposta que a tela de navegacao ja consome. Ver D-71.
 */
public class ProdutoResponse {

    private UUID id;
    private String sku;
    private String nome;
    private String descricao;
    private String imagemUrl;
    private BigDecimal preco;
    private int saldoEstoque;
    private UUID pontoMapaId;

    /** Corredor onde o produto esta. Nulo se o produto nao tiver ponto no mapa. */
    private String corredor;

    public ProdutoResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public int getSaldoEstoque() {
        return saldoEstoque;
    }

    public void setSaldoEstoque(int saldoEstoque) {
        this.saldoEstoque = saldoEstoque;
    }

    public UUID getPontoMapaId() {
        return pontoMapaId;
    }

    public void setPontoMapaId(UUID pontoMapaId) {
        this.pontoMapaId = pontoMapaId;
    }

    public String getCorredor() {
        return corredor;
    }

    public void setCorredor(String corredor) {
        this.corredor = corredor;
    }

    public static ProdutoResponse de(Produto produto) {
        ProdutoResponse resposta = MapperFactory.getMapper().map(produto, ProdutoResponse.class);
        PontoMapa ponto = produto.getPontoMapa();
        resposta.setPontoMapaId(ponto != null ? ponto.getId() : null);
        resposta.setCorredor(ponto != null ? ponto.getCorredor() : null);
        return resposta;
    }
}
