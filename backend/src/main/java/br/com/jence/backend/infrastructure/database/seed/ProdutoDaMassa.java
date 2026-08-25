package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Um produto da massa de demonstracao, com tudo que ele precisa para existir.
 * <p>
 * <b>Uma entrada por produto, e nao um arquivo por aspecto.</b> Antes, nome e preco viviam no
 * carregador, a descricao ao lado deles e as caracteristicas noutro arquivo - acrescentar um
 * produto exigia editar dois lugares em paralelo, e esquecer metade nao dava erro nenhum:
 * simplesmente sumia do filtro. Ver D-66.
 *
 * @param sku        identificador unico, no formato {@code SKU-XXX-000}
 * @param nome       como aparece na busca e na lista
 * @param secao      corredor da loja; precisa existir em {@code PlantaDaLoja}
 * @param preco      valor em reais
 * @param estoque    saldo inicial; zero apenas no produto que encena a ruptura
 * @param descricao  o que o produto e e para que serve
 * @param atributos  caracteristicas pelas quais ele pode ser filtrado
 */
public record ProdutoDaMassa(String sku, String nome, String secao, String preco, int estoque,
                             String descricao, List<ValorDeAtributo> atributos) {

    public BigDecimal precoEmReais() {
        return new BigDecimal(preco);
    }

    /** Acucar para declarar uma caracteristica sem repetir o nome do tipo. */
    static ValorDeAtributo de(AtributoProduto atributo, String valor) {
        return new ValorDeAtributo(atributo, valor);
    }
}
