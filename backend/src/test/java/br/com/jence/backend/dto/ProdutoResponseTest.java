package br.com.jence.backend.dto;

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

        assertEquals("Tintas", resposta.getCorredor());
        assertEquals(tintas.getId(), resposta.getPontoMapaId(),
                "o id continua vindo: o mapa ainda precisa dele para casar o marcador");
    }

    @Test
    @DisplayName("copia cada campo do produto, e o ponto vira id e corredor")
    void copiaOsCampos() {
        /*
         * O ModelMapper casa os campos pelo nome. Este teste confere campo a campo para que
         * um campo novo com nome parecido nao seja preenchido com o valor errado sem ninguem ver.
         */
        PontoMapa tintas = new PontoMapa(
                UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10);
        Produto produto = new Produto(UUID.randomUUID(), "SKU-TIN-001", "Tinta Acrílica Fosca Branca 18L",
                "Tinta de acabamento fosco.", "https://exemplo.com/tinta.png",
                new BigDecimal("289.90"), 12, tintas);

        ProdutoResponse resposta = ProdutoResponse.de(produto);

        assertEquals(produto.getId(), resposta.getId());
        assertEquals("SKU-TIN-001", resposta.getSku());
        assertEquals("Tinta Acrílica Fosca Branca 18L", resposta.getNome());
        assertEquals("Tinta de acabamento fosco.", resposta.getDescricao());
        assertEquals("https://exemplo.com/tinta.png", resposta.getImagemUrl());
        assertEquals(new BigDecimal("289.90"), resposta.getPreco());
        assertEquals(12, resposta.getSaldoEstoque());
        assertEquals(tintas.getId(), resposta.getPontoMapaId());
        assertEquals("Tintas", resposta.getCorredor());
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
        assertEquals(detalhe.pontoMapa().getCorredor(), detalhe.corredor(),
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

        assertNull(resposta.getCorredor());
        assertNull(resposta.getPontoMapaId());
        assertEquals("Tinta Acrílica Fosca Branca 18L", resposta.getNome());
    }
}
