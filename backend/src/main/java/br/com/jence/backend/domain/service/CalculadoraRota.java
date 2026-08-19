package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Define a ordem de visita dos itens do roteiro pela loja, usando a heuristica do vizinho
 * mais proximo (Nearest Neighbor).
 * <p>
 * Regra de negocio pura: nao conhece banco, HTTP nem framework, e por isso vive no dominio
 * e e testavel sem subir a aplicacao.
 */
public final class CalculadoraRota {

    private CalculadoraRota() {
    }

    /**
     * Ordena os itens partindo de {@code origem}, sempre seguindo para o item mais proximo
     * do ponto atual, e grava a posicao de cada um em {@code ordemCaminho} (comecando em 1).
     * <p>
     * A origem e parametro em vez de constante porque o ponto de partida muda conforme o
     * contexto: no handoff e o totem da entrada; no tratamento de ruptura de estoque, sera a
     * posicao onde o cliente esta naquele momento.
     *
     * @return os itens na ordem em que devem ser visitados
     */
    public static List<ItemRoteiro> calcularRota(PontoMapa origem, List<ItemRoteiro> itens) {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }

        List<ItemRoteiro> restantes = new ArrayList<>(itens);
        List<ItemRoteiro> rota = new ArrayList<>(itens.size());

        PontoMapa atual = origem;
        int ordem = 1;

        while (!restantes.isEmpty()) {
            ItemRoteiro proximo = maisProximoDe(atual, restantes);

            proximo.definirOrdem(ordem++);
            rota.add(proximo);
            restantes.remove(proximo);

            // Itens que dividem o mesmo ponto de mapa ficam a distancia zero daqui, entao a
            // proxima iteracao os escolhe naturalmente: o cliente resolve o corredor inteiro
            // de uma vez em vez de voltar nele depois.
            atual = proximo.getProduto().getPontoMapa();
        }

        return rota;
    }

    private static ItemRoteiro maisProximoDe(PontoMapa referencia, List<ItemRoteiro> candidatos) {
        return candidatos.stream()
                .min(Comparator.comparingDouble(
                        item -> referencia.calcularDistanciaPara(item.getProduto().getPontoMapa())))
                .orElseThrow();
    }

    /** Distancia total percorrida ao seguir a rota a partir da origem. */
    public static double distanciaTotal(PontoMapa origem, List<ItemRoteiro> rota) {
        double total = 0;
        PontoMapa atual = origem;

        for (ItemRoteiro item : rota) {
            PontoMapa destino = item.getProduto().getPontoMapa();
            total += atual.calcularDistanciaPara(destino);
            atual = destino;
        }

        return total;
    }
}
