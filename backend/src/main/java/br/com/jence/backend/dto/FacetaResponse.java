package br.com.jence.backend.dto;

import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.FacetaDeProdutos;

import java.util.List;

/**
 * Espelha o schema {@code Faceta}: um filtro disponivel na tela, com os valores presentes no
 * resultado atual.
 * <p>
 * O rotulo vai junto da chave para o frontend nao precisar manter a traducao de cada
 * caracteristica - se um atributo novo entrar, ele aparece na tela sozinho.
 */
public class FacetaResponse {

    private AtributoProduto atributo;
    private String rotulo;
    private List<ValorResponse> valores;

    public FacetaResponse() {
    }

    public AtributoProduto getAtributo() {
        return atributo;
    }

    public void setAtributo(AtributoProduto atributo) {
        this.atributo = atributo;
    }

    public String getRotulo() {
        return rotulo;
    }

    public void setRotulo(String rotulo) {
        this.rotulo = rotulo;
    }

    public List<ValorResponse> getValores() {
        return valores;
    }

    public void setValores(List<ValorResponse> valores) {
        this.valores = valores;
    }

    /** Um valor da faceta e quantos produtos do resultado o tem. */
    public static class ValorResponse {

        private String valor;
        private long quantidade;

        public ValorResponse() {
        }

        public String getValor() {
            return valor;
        }

        public void setValor(String valor) {
            this.valor = valor;
        }

        public long getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(long quantidade) {
            this.quantidade = quantidade;
        }

        public static ValorResponse de(FacetaDeProdutos.Valor valor) {
            ValorResponse resposta = new ValorResponse();
            resposta.setValor(valor.valor());
            resposta.setQuantidade(valor.quantidade());
            return resposta;
        }
    }

    public static FacetaResponse de(FacetaDeProdutos faceta) {
        FacetaResponse resposta = new FacetaResponse();
        resposta.setAtributo(faceta.atributo());
        resposta.setRotulo(faceta.rotulo());
        resposta.setValores(faceta.valores().stream().map(ValorResponse::de).toList());
        return resposta;
    }
}
