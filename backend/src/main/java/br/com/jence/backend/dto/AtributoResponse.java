package br.com.jence.backend.dto;

import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.ValorDeAtributo;

/** Espelha o schema {@code Atributo}: uma linha da tabela de especificacoes do produto. */
public class AtributoResponse {

    private AtributoProduto atributo;
    private String rotulo;
    private String valor;

    public AtributoResponse() {
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

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public static AtributoResponse de(ValorDeAtributo valor) {
        AtributoResponse resposta = new AtributoResponse();
        resposta.setAtributo(valor.atributo());
        resposta.setRotulo(valor.atributo().getRotulo());
        resposta.setValor(valor.valor());
        return resposta;
    }
}
