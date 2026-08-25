package br.com.jence.backend.application.dto;

import java.util.List;

/**
 * A planta da loja pronta para desenhar.
 * <p>
 * Tudo no mesmo grid 0-100 em que vivem os produtos, as placas e a posicao do cliente - entao
 * o frontend desenha as tres camadas com a mesma escala, sem converter nada.
 *
 * @param blocos os corredores, como retangulos com rotulo
 * @param pontos o que nao e prateleira: caixas, banheiro e as placas de QR Code, cada um com
 *               seu tipo, para o frontend escolher o que desenhar
 */
public record MapaResponse(
        int largura,
        int altura,
        List<BlocoMapaResponse> blocos,
        List<PontoMapaResponse> pontos
) {
}
