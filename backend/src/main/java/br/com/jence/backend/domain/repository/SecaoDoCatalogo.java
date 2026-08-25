package br.com.jence.backend.domain.repository;

/**
 * Uma secao da loja como ela aparece no menu de navegacao do catalogo.
 *
 * @param nome              o corredor, igual ao rotulo do bloco no mapa
 * @param quantidadeProdutos quantos produtos ela tem hoje
 */
public record SecaoDoCatalogo(String nome, long quantidadeProdutos) {
}
