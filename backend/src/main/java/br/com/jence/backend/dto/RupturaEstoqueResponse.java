package br.com.jence.backend.dto;

import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.Produto;

import java.util.UUID;

/**
 * Espelha o schema {@code RupturaEstoqueResponse} do contrato OpenAPI.
 * <p>
 * O campo {@code origemSugestao} nao estava no contrato original e foi acrescentado: o
 * frontend precisa saber se a recomendacao veio do assistente ou do calculo de proximidade
 * para nao rotular como "sugestao inteligente" o que foi apenas o item disponivel mais perto.
 */
public class RupturaEstoqueResponse {

    private UUID produtoOriginalId;
    private UUID produtoSugeridoId;
    private ProdutoDetalhadoResponse produtoSugerido;
    private String justificativa;
    private OrigemSugestao origemSugestao;

    public RupturaEstoqueResponse() {
    }

    public UUID getProdutoOriginalId() {
        return produtoOriginalId;
    }

    public void setProdutoOriginalId(UUID produtoOriginalId) {
        this.produtoOriginalId = produtoOriginalId;
    }

    public UUID getProdutoSugeridoId() {
        return produtoSugeridoId;
    }

    public void setProdutoSugeridoId(UUID produtoSugeridoId) {
        this.produtoSugeridoId = produtoSugeridoId;
    }

    public ProdutoDetalhadoResponse getProdutoSugerido() {
        return produtoSugerido;
    }

    public void setProdutoSugerido(ProdutoDetalhadoResponse produtoSugerido) {
        this.produtoSugerido = produtoSugerido;
    }

    public String getJustificativa() {
        return justificativa;
    }

    public void setJustificativa(String justificativa) {
        this.justificativa = justificativa;
    }

    public OrigemSugestao getOrigemSugestao() {
        return origemSugestao;
    }

    public void setOrigemSugestao(OrigemSugestao origemSugestao) {
        this.origemSugestao = origemSugestao;
    }

    public static RupturaEstoqueResponse de(UUID produtoOriginalId, Produto sugerido,
                                            String justificativa, OrigemSugestao origemSugestao) {
        RupturaEstoqueResponse resposta = new RupturaEstoqueResponse();
        resposta.setProdutoOriginalId(produtoOriginalId);
        resposta.setProdutoSugeridoId(sugerido.getId());
        resposta.setProdutoSugerido(ProdutoDetalhadoResponse.de(sugerido));
        resposta.setJustificativa(justificativa);
        resposta.setOrigemSugestao(origemSugestao);
        return resposta;
    }
}
