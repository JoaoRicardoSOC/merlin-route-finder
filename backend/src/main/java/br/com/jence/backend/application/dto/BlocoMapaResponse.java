package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.BlocoMapa;

/** Espelha o schema {@code BlocoMapa} do contrato: um retangulo a desenhar na tela do mapa. */
public record BlocoMapaResponse(
        String rotulo,
        int x,
        int y,
        int largura,
        int altura
) {
    public static BlocoMapaResponse de(BlocoMapa bloco) {
        return new BlocoMapaResponse(
                bloco.rotulo(), bloco.x(), bloco.y(), bloco.largura(), bloco.altura());
    }
}
