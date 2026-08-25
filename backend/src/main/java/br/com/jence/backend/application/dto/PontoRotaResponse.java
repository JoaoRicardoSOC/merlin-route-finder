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
        return de(item, item.getOrdemCaminho());
    }

    public static PontoRotaResponse de(ItemRoteiro item, Integer ordem) {
        return new PontoRotaResponse(
                ordem,
                ItemRoteiroDetalhadoResponse.de(item),
                PontoMapaResponse.de(item.getProduto().getPontoMapa())
        );
    }
}
