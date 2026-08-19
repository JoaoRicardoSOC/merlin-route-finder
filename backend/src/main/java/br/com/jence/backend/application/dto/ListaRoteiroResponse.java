package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ListaRoteiro;

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
        // Usa a ordem de caminho quando ja existe (pos-handoff); antes disso, os itens vem
        // com ordem nula e getItensOrdenados os mantem no fim sem quebrar.
        List<ItemRoteiroDetalhadoResponse> itens = lista.getItensOrdenados().stream()
                .map(ItemRoteiroDetalhadoResponse::de)
                .toList();

        return new ListaRoteiroResponse(lista.getId(), lista.getSessaoId(), itens.size(), itens);
    }
}
