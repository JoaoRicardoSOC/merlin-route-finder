package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ItemRoteiro;

/**
 * Espelha o schema {@code PontoRota} do contrato OpenAPI: uma parada na sequencia de
 * navegacao, com o item a coletar e onde ele esta.
 */
public record PontoRotaResponse(
        Integer ordem,
        ItemRoteiroDetalhadoResponse item,
        PontoMapaResponse pontoMapa
) {
    public static PontoRotaResponse de(ItemRoteiro item) {
        return new PontoRotaResponse(
                item.getOrdemCaminho(),
                ItemRoteiroDetalhadoResponse.de(item),
                PontoMapaResponse.de(item.getProduto().getPontoMapa())
        );
    }
}
