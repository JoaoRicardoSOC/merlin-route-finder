package br.com.jence.backend.domain.entity;

/**
 * Uma caracteristica de um produto: a chave e o valor que ele tem para ela.
 *
 * @param atributo qual caracteristica
 * @param valor    o valor, ja no formato em que aparece na tela ("25 mm", "Tigre", "120")
 */
public record ValorDeAtributo(AtributoProduto atributo, String valor) {

    public ValorDeAtributo {
        if (atributo == null) {
            throw new IllegalArgumentException("Atributo nao pode ser nulo");
        }
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(
                    "Valor de %s nao pode ser vazio".formatted(atributo));
        }
        valor = valor.trim();
    }
}
