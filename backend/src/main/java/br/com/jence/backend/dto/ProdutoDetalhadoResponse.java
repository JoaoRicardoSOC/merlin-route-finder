package br.com.jence.backend.dto;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.ValorDeAtributo;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import br.com.jence.backend.factory.MapperFactory;

/**
 * Espelha o schema {@code ProdutoDetalhado} do contrato OpenAPI.
 * <p>
 * Os campos sao repetidos em vez de compostos a partir de {@link ProdutoResponse} porque o
 * contrato usa {@code allOf}, que resulta num objeto JSON plano. Compor geraria um
 * {@code produto} aninhado, divergindo do contrato.
 */
public class ProdutoDetalhadoResponse {

    private UUID id;
    private String sku;
    private String nome;
    private String descricao;
    private String imagemUrl;
    private BigDecimal preco;
    private int saldoEstoque;
    private UUID pontoMapaId;

    /**
     * O mesmo corredor que a listagem leva.
     *
     * <p>Redundante com {@code pontoMapa.corredor}, e de proposito: o contrato declara
     * {@code ProdutoDetalhado} como um {@code allOf} de {@code Produto}, entao herda o
     * campo e promete entrega-lo. Sem ele aqui, o contrato mentiria - e mentiu por um dia,
     * ate a tela da ruptura receber a sugestao com o corredor vazio. Ver D-71.
     */
    private String corredor;

    private PontoMapaResponse pontoMapa;
    private List<AtributoResponse> atributos;

    public ProdutoDetalhadoResponse() {
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

    public PontoMapaResponse getPontoMapa() {
        return pontoMapa;
    }

    public void setPontoMapa(PontoMapaResponse pontoMapa) {
        this.pontoMapa = pontoMapa;
    }

    public List<AtributoResponse> getAtributos() {
        return atributos;
    }

    public void setAtributos(List<AtributoResponse> atributos) {
        this.atributos = atributos;
    }

    /** Detalhe sem especificacoes: usado onde o produto aparece embutido em outra resposta. */
    public static ProdutoDetalhadoResponse de(Produto produto) {
        return de(produto, List.of());
    }

    public static ProdutoDetalhadoResponse de(Produto produto, List<ValorDeAtributo> atributos) {
        ProdutoDetalhadoResponse resposta =
                MapperFactory.getMapper().map(produto, ProdutoDetalhadoResponse.class);
        PontoMapa ponto = produto.getPontoMapa();
        resposta.setPontoMapaId(ponto != null ? ponto.getId() : null);
        resposta.setCorredor(ponto != null ? ponto.getCorredor() : null);
        resposta.setPontoMapa(PontoMapaResponse.de(ponto));
        resposta.setAtributos(atributos.stream().map(AtributoResponse::de).toList());
        return resposta;
    }
}
