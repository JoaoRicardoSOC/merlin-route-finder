package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ListaRoteiro;

import java.util.List;
import java.util.UUID;

/**
 * Espelha o schema {@code RotaCalculada} do contrato OpenAPI. E o que o celular recebe ao
 * escanear o QR Code: a sequencia de paradas com coordenadas, suficiente para desenhar o
 * mapa e conduzir a navegacao.
 */
public record RotaCalculadaResponse(
        UUID sessaoId,
        UUID listaRoteiroId,
        List<PontoRotaResponse> pontos
) {
    public static RotaCalculadaResponse de(ListaRoteiro lista) {
        return new RotaCalculadaResponse(
                lista.getSessaoId(),
                lista.getId(),
                lista.getItensOrdenados().stream().map(PontoRotaResponse::de).toList()
        );
    }
}
