package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.OrigemSugestao;
import br.com.jence.backend.domain.entity.Produto;

import java.util.UUID;

/**
 * Espelha o schema {@code RupturaEstoqueResponse} do contrato OpenAPI.
 * <p>
 * O campo {@code origemSugestao} nao estava no contrato original e foi acrescentado: o
 * frontend precisa saber se a recomendacao veio do assistente ou do calculo de proximidade
 * para nao rotular como "sugestao inteligente" o que foi apenas o item disponivel mais perto.
 */
public record RupturaEstoqueResponse(
        UUID produtoOriginalId,
        UUID produtoSugeridoId,
        ProdutoDetalhadoResponse produtoSugerido,
        String justificativa,
        OrigemSugestao origemSugestao
) {
    public static RupturaEstoqueResponse de(UUID produtoOriginalId, Produto sugerido,
                                            String justificativa, OrigemSugestao origemSugestao) {
        return new RupturaEstoqueResponse(
                produtoOriginalId,
                sugerido.getId(),
                ProdutoDetalhadoResponse.de(sugerido),
                justificativa,
                origemSugestao
        );
    }
}
