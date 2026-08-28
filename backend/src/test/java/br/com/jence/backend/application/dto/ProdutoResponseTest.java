package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A conversao de produto para a resposta da listagem.
 * <p>
 * Existe por causa de um campo so: o corredor. Sem ele a tela de navegacao nao tinha como
 * dizer onde o produto fica, porque a listagem levava apenas o id do ponto - e o que o cliente
 * via, em todos os cards, era um texto generico no lugar do corredor. Ver D-71.
 */
class ProdutoResponseTest {

    private Produto produtoEm(PontoMapa ponto) {
        return new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrílica Fosca Branca 18L",
                "Tinta de acabamento fosco.", null, new BigDecimal("289.90"), 12, ponto);
    }

    @Test
    @DisplayName("o corredor do ponto chega na resposta, junto do id")
    void corredorChegaNaResposta() {
        PontoMapa tintas = new PontoMapa(
                UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

        ProdutoResponse resposta = ProdutoResponse.de(produtoEm(tintas));

        assertThat(resposta.corredor()).isEqualTo("Tintas");
        assertThat(resposta.pontoMapaId())
                .as("o id continua vindo: o mapa ainda precisa dele para casar o marcador")
                .isEqualTo(tintas.getId());
    }

    @Test
    @DisplayName("o detalhe leva o mesmo corredor, porque o contrato promete")
    void detalheTambemLevaOCorredor() {
        /*
         * O contrato declara ProdutoDetalhado como um allOf de Produto, entao ele herda o
         * campo e promete entrega-lo. Ficou um dia sem entregar, e so apareceu quando a tela
         * da ruptura recebeu a sugestao com o corredor vazio - nenhuma tela quebrou, porque
         * todas leem pontoMapa.corredor antes. O contrato e que mentia.
         */
        PontoMapa tintas = new PontoMapa(
                UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);

        ProdutoDetalhadoResponse detalhe = ProdutoDetalhadoResponse.de(produtoEm(tintas));

        assertThat(detalhe.corredor()).isEqualTo("Tintas");
        assertThat(detalhe.corredor())
                .as("os dois caminhos precisam concordar")
                .isEqualTo(detalhe.pontoMapa().corredor());
    }

    @Test
    @DisplayName("produto sem ponto no mapa devolve corredor nulo, e nao quebra")
    void semPontoNaoQuebra() {
        /*
         * Produto sem ponto e estado possivel - a carga avisa e segue quando uma secao nao
         * existe na planta. A listagem precisa continuar respondendo, com o campo nulo, para
         * a tela tratar a ausencia em vez de receber um erro.
         */
        ProdutoResponse resposta = ProdutoResponse.de(produtoEm(null));

        assertThat(resposta.corredor()).isNull();
        assertThat(resposta.pontoMapaId()).isNull();
        assertThat(resposta.nome()).isEqualTo("Tinta Acrílica Fosca Branca 18L");
    }
}
