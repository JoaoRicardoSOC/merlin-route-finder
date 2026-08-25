package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;

/** Espelha o schema {@code Atributo}: uma linha da tabela de especificacoes do produto. */
public record AtributoResponse(AtributoProduto atributo, String rotulo, String valor) {

    public static AtributoResponse de(ValorDeAtributo valor) {
        return new AtributoResponse(
                valor.atributo(), valor.atributo().getRotulo(), valor.valor());
    }
}
