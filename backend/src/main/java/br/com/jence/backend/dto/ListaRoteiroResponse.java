package br.com.jence.backend.dto;

import br.com.jence.backend.model.ListaRoteiro;

import java.util.List;
import java.util.UUID;

/** Espelha o schema {@code ListaRoteiroResponse} do contrato OpenAPI. */
public record ListaRoteiroResponse(
        UUID id,
        UUID sessaoId,
        int quantidadeItens,
        List<ItemRoteiroDetalhadoResponse> itens
) {
    public static ListaRoteiroResponse de(ListaRoteiro lista) {
        List<ItemRoteiroDetalhadoResponse> itens = lista.getItensParaExibicao().stream()
                .map(ItemRoteiroDetalhadoResponse::de)
                .toList();

        return new ListaRoteiroResponse(lista.getId(), lista.getSessaoId(), itens.size(), itens);
    }
}
