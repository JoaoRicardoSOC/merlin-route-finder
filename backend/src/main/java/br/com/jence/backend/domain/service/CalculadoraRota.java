package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Define a ordem de visita dos itens do roteiro pela loja.
 * <p>
 * Duas etapas: uma heuristica construtiva (vizinho mais proximo) monta um caminho razoavel, e
 * uma melhoria local (2-opt) desfaz os cruzamentos que a primeira deixou para tras. Ver D-43.
 * <p>
 * Regra de negocio pura: nao conhece banco, HTTP nem framework, e por isso vive no dominio
 * e e testavel sem subir a aplicacao.
 */
public final class CalculadoraRota {

    /*
     * Teto de passadas do 2-opt. Cada passada e O(n^2) e so continua enquanto houver ganho,
     * entao na pratica o laco para sozinho em poucas rodadas. O teto existe para que uma lista
     * enorme - o carrinho sem limite da D-17 - nunca prenda a geracao do QR Code.
     */
    private static final int MAXIMO_DE_PASSADAS = 50;

    /*
     * As distancias sao raizes quadradas, e comparar dois double por "menor que" puro faria o
     * algoritmo aceitar um ganho de 1e-15 e trocar de lugar para sempre, alternando entre dois
     * caminhos de mesmo comprimento. So conta como melhoria o que for maior que este limiar.
     */
    private static final double GANHO_MINIMO = 1e-9;

    private CalculadoraRota() {
    }

    /**
     * Ordena os itens partindo de {@code origem} e grava a posicao de cada um em
     * {@code ordemCaminho} (comecando em 1).
     * <p>
     * A origem e parametro em vez de constante porque o ponto de partida muda conforme o
     * contexto: na montagem da rota e o ponto de partida; no tratamento de ruptura, e a
     * posicao onde o cliente esta naquele momento.
     *
     * @return os itens na ordem em que devem ser visitados
     */
    public static List<ItemRoteiro> calcularRota(PontoMapa origem, List<ItemRoteiro> itens) {
        if (itens == null || itens.isEmpty()) {
            return List.of();
        }

        List<ItemRoteiro> rota = construirPorVizinhoMaisProximo(origem, itens);
        refinarCom2Opt(origem, rota);
        numerar(rota);

        return List.copyOf(rota);
    }

    // ---------------------------------------------------------------- etapa 1: construcao

    /**
     * Heuristica do vizinho mais proximo: do ponto atual, segue sempre para o item mais perto.
     * <p>
     * Publica para que a melhoria trazida pelo 2-opt possa ser medida contra ela.
     */
    public static List<ItemRoteiro> construirPorVizinhoMaisProximo(PontoMapa origem, List<ItemRoteiro> itens) {
        if (itens == null || itens.isEmpty()) {
            return new ArrayList<>();
        }

        List<ItemRoteiro> restantes = new ArrayList<>(itens);
        List<ItemRoteiro> rota = new ArrayList<>(itens.size());

        PontoMapa atual = origem;

        while (!restantes.isEmpty()) {
            ItemRoteiro proximo = maisProximoDe(atual, restantes);

            rota.add(proximo);
            restantes.remove(proximo);

            // Itens que dividem o mesmo ponto de mapa ficam a distancia zero daqui, entao a
            // proxima iteracao os escolhe naturalmente: o cliente resolve o corredor inteiro
            // de uma vez em vez de voltar nele depois.
            atual = proximo.getProduto().getPontoMapa();
        }

        numerar(rota);
        return rota;
    }

    private static ItemRoteiro maisProximoDe(PontoMapa referencia, List<ItemRoteiro> candidatos) {
        return candidatos.stream()
                .min(Comparator.comparingDouble(
                        item -> referencia.calcularDistanciaPara(item.getProduto().getPontoMapa())))
                .orElseThrow();
    }

    // ---------------------------------------------------------------- etapa 2: melhoria local

    /*
     * 2-opt: procura dois trechos que se cruzam e inverte o pedaco entre eles, o que desfaz o
     * cruzamento. Repete enquanto encontrar ganho.
     *
     * A variante aqui e a de CAMINHO ABERTO, nao a de ciclo fechado que costuma ilustrar o
     * algoritmo. O cliente parte de um ponto fixo - o totem - mas nao precisa voltar a ele, e
     * por isso a inversao que termina no ultimo item troca apenas a aresta de entrada do
     * trecho: nao existe aresta de saida para recalcular. Tratar como ciclo faria o algoritmo
     * otimizar um retorno a entrada da loja que ninguem vai percorrer.
     */
    private static void refinarCom2Opt(PontoMapa origem, List<ItemRoteiro> rota) {
        int n = rota.size();
        if (n < 3) {
            // Com dois itens so ha uma inversao possivel, e o vizinho mais proximo ja escolheu
            // a melhor das duas ordens ao partir da origem.
            return;
        }

        for (int passada = 0; passada < MAXIMO_DE_PASSADAS; passada++) {
            boolean melhorou = false;

            for (int i = 0; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    if (ganhoDaInversao(origem, rota, i, j) > GANHO_MINIMO) {
                        Collections.reverse(rota.subList(i, j + 1));
                        melhorou = true;
                    }
                }
            }

            if (!melhorou) {
                return;
            }
        }
    }

    /*
     * Quanto se economiza ao inverter o trecho [i..j]. A inversao troca no maximo duas
     * arestas: a que entra no trecho e a que sai dele. Todas as arestas internas continuam
     * existindo, apenas percorridas ao contrario - e como a distancia e simetrica, elas se
     * cancelam e nao entram na conta.
     */
    private static double ganhoDaInversao(PontoMapa origem, List<ItemRoteiro> rota, int i, int j) {
        PontoMapa anterior = (i == 0) ? origem : pontoDe(rota, i - 1);
        PontoMapa inicioDoTrecho = pontoDe(rota, i);
        PontoMapa fimDoTrecho = pontoDe(rota, j);

        double antes = anterior.calcularDistanciaPara(inicioDoTrecho);
        double depois = anterior.calcularDistanciaPara(fimDoTrecho);

        // Se o trecho vai ate o fim da rota nao ha aresta de saida: o caminho e aberto.
        if (j < rota.size() - 1) {
            PontoMapa seguinte = pontoDe(rota, j + 1);
            antes += fimDoTrecho.calcularDistanciaPara(seguinte);
            depois += inicioDoTrecho.calcularDistanciaPara(seguinte);
        }

        return antes - depois;
    }

    private static PontoMapa pontoDe(List<ItemRoteiro> rota, int indice) {
        return rota.get(indice).getProduto().getPontoMapa();
    }

    private static void numerar(List<ItemRoteiro> rota) {
        for (int i = 0; i < rota.size(); i++) {
            rota.get(i).definirOrdem(i + 1);
        }
    }

    // ---------------------------------------------------------------- medicao

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
