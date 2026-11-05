package br.com.jence.backend.dto;

import br.com.jence.backend.model.SecaoDoCatalogo;

/**
 * Espelha o schema {@code Secao} do contrato: uma entrada do menu de navegacao do catalogo.
 * <p>
 * A quantidade vai junto porque e o que faz um menu valer: "Tintas (5)" diz ao cliente se
 * vale entrar antes de ele gastar um toque para descobrir.
 */
public class SecaoResponse {

    private String nome;
    private long quantidadeProdutos;

    public SecaoResponse() {
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public long getQuantidadeProdutos() {
        return quantidadeProdutos;
    }

    public void setQuantidadeProdutos(long quantidadeProdutos) {
        this.quantidadeProdutos = quantidadeProdutos;
    }

    public static SecaoResponse de(SecaoDoCatalogo secao) {
        SecaoResponse resposta = new SecaoResponse();
        resposta.setNome(secao.nome());
        resposta.setQuantidadeProdutos(secao.quantidadeProdutos());
        return resposta;
    }
}
