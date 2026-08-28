package br.com.jence.backend.domain.entity;

/**
 * As caracteristicas pelas quais um produto pode ser filtrado.
 * <p>
 * <b>A ordem de declaracao e a ordem de exibicao</b> dos filtros na tela, e nao e alfabetica:
 * marca vem primeiro porque e o filtro mais usado numa loja de construcao, e as medidas vem
 * depois das caracteristicas gerais. Ver D-62.
 * <p>
 * Um enum, e nao texto livre, por tres motivos: o rotulo de exibicao vive junto da chave, a
 * massa nao pode gravar "Marca" e "marca" como coisas diferentes, e a lista fechada permite
 * ordenar os filtros de forma previsivel.
 */
public enum AtributoProduto {

    MARCA("Marca"),
    TIPO("Tipo"),
    MATERIAL("Material"),
    COR("Cor"),
    TEMPERATURA_DE_COR("Temperatura de cor"),
    ACABAMENTO("Acabamento"),
    FORMATO("Formato"),
    POTENCIA("Potência"),
    AMPERAGEM("Amperagem"),
    POLOS("Polos"),
    GRAO("Grão"),
    BITOLA("Bitola"),
    VOLUME("Volume"),
    PESO("Peso"),
    COMPRIMENTO("Comprimento"),
    LARGURA("Largura"),
    DIMENSAO("Dimensão"),
    QUANTIDADE("Quantidade");

    private final String rotulo;

    AtributoProduto(String rotulo) {
        this.rotulo = rotulo;
    }

    /** Como o filtro aparece na tela. */
    public String getRotulo() {
        return rotulo;
    }
}
