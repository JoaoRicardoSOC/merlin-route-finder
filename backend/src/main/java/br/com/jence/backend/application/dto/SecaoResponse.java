package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.repository.SecaoDoCatalogo;

/**
 * Espelha o schema {@code Secao} do contrato: uma entrada do menu de navegacao do catalogo.
 * <p>
 * A quantidade vai junto porque e o que faz um menu valer: "Tintas (5)" diz ao cliente se
 * vale entrar antes de ele gastar um toque para descobrir.
 */
public record SecaoResponse(String nome, long quantidadeProdutos) {

    public static SecaoResponse de(SecaoDoCatalogo secao) {
        return new SecaoResponse(secao.nome(), secao.quantidadeProdutos());
    }
}
