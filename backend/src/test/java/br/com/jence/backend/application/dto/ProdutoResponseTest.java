package br.com.jence.backend.application.dto;

import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        assertEquals("Tintas", resposta.corredor());
        assertEquals(tintas.getId(), resposta.pontoMapaId(),
                "o id continua vindo: o mapa ainda precisa dele para casar o marcador");
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

        assertEquals("Tintas", detalhe.corredor());
        assertEquals(detalhe.pontoMapa().corredor(), detalhe.corredor(),
                "os dois caminhos precisam concordar");
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

        assertNull(resposta.corredor());
        assertNull(resposta.pontoMapaId());
        assertEquals("Tinta Acrílica Fosca Branca 18L", resposta.nome());
    }
}
