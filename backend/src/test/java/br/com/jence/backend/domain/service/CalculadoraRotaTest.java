package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do algoritmo de roteamento. Nao dependem de banco nem de contexto Spring: rodam com
 * {@code ./mvnw test} em qualquer maquina, sem configuracao.
 */
class CalculadoraRotaTest {

    /** Coordenadas equivalentes as da massa de demonstracao, seguindo a planta real da loja. */
    private static final PontoMapa TOTEM = ponto(TipoPonto.TOTEM, "Entrada", 50, 95);
    private static final PontoMapa TINTAS = ponto(TipoPonto.PRATELEIRA, "Tintas", 32, 10);
    private static final PontoMapa ENCANAMENTO = ponto(TipoPonto.PRATELEIRA, "Encanamento", 48, 30);
    private static final PontoMapa JARDIM = ponto(TipoPonto.PRATELEIRA, "Jardim", 36, 50);
    private static final PontoMapa MATERIAIS = ponto(TipoPonto.PRATELEIRA, "Materiais de construcao", 14, 80);
    private static final PontoMapa DECORACAO = ponto(TipoPonto.PRATELEIRA, "Decoracao", 88, 55);
    private static final PontoMapa ILUMINACAO = ponto(TipoPonto.PRATELEIRA, "Iluminacao", 76, 32);
    private static final PontoMapa FERRAMENTAS = ponto(TipoPonto.PRATELEIRA, "Ferramentas", 20, 55);
    private static final PontoMapa COZINHAS = ponto(TipoPonto.PRATELEIRA, "Cozinhas", 62, 30);
    private static final PontoMapa FERRAGENS = ponto(TipoPonto.PRATELEIRA, "Ferragens", 22, 32);
    private static final PontoMapa ELETRICA = ponto(TipoPonto.PRATELEIRA, "Eletrica", 34, 30);

    /** As dez secoes da massa de demonstracao, para as medicoes sobre a planta real. */
    private static final List<PontoMapa> SECOES = List.of(
            TINTAS, FERRAGENS, ELETRICA, ENCANAMENTO, COZINHAS,
            ILUMINACAO, JARDIM, FERRAMENTAS, DECORACAO, MATERIAIS);

    private static PontoMapa ponto(TipoPonto tipo, String corredor, int x, int y) {
        return new PontoMapa(UUID.randomUUID(), tipo, corredor, x, y);
    }

    private static ItemRoteiro item(String nome, PontoMapa ponto) {
        Produto produto = new Produto(UUID.randomUUID(), "SKU-" + nome, nome,
                new BigDecimal("10.00"), 5, ponto);
        return new ItemRoteiro(UUID.randomUUID(), produto);
    }

    @Test
    @DisplayName("lista vazia devolve rota vazia")
    void listaVazia() {
        assertThat(CalculadoraRota.calcularRota(TOTEM, List.of())).isEmpty();
        assertThat(CalculadoraRota.calcularRota(TOTEM, null)).isEmpty();
    }

    @Test
    @DisplayName("item unico recebe ordem 1")
    void itemUnico() {
        ItemRoteiro tinta = item("Tinta", TINTAS);

        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(TOTEM, List.of(tinta));

        assertThat(rota).containsExactly(tinta);
        assertThat(tinta.getOrdemCaminho()).isEqualTo(1);
    }

    @Test
    @DisplayName("comeca pelo item mais proximo da origem")
    void comecaPeloMaisProximo() {
        // Da entrada (50,95): Materiais (14,80) esta a ~39 e Tintas (32,10) a ~87.
        ItemRoteiro tinta = item("Tinta", TINTAS);
        ItemRoteiro cimento = item("Cimento", MATERIAIS);

        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(TOTEM, List.of(tinta, cimento));

        assertThat(rota.get(0)).isEqualTo(cimento);
        assertThat(cimento.getOrdemCaminho()).isEqualTo(1);
        assertThat(tinta.getOrdemCaminho()).isEqualTo(2);
    }

    @Test
    @DisplayName("itens no mesmo corredor sao visitados em sequencia")
    void agrupaItensDoMesmoPonto() {
        // Alternados de proposito na entrada, para provar que o algoritmo os agrupa.
        ItemRoteiro tintaA = item("TintaA", TINTAS);
        ItemRoteiro cano = item("Cano", ENCANAMENTO);
        ItemRoteiro tintaB = item("TintaB", TINTAS);
        ItemRoteiro cola = item("Cola", ENCANAMENTO);

        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(TOTEM, List.of(tintaA, cano, tintaB, cola));

        List<String> corredores = rota.stream()
                .map(i -> i.getProduto().getPontoMapa().getCorredor())
                .toList();

        assertThat(corredores)
                .as("o cliente nao pode ir a Tintas, sair e voltar depois")
                .containsExactly("Encanamento", "Encanamento", "Tintas", "Tintas");
    }

    @Test
    @DisplayName("todos os itens recebem ordem sequencial, sem repeticao nem buraco")
    void ordemSequencialCompleta() {
        List<ItemRoteiro> itens = List.of(
                item("Tinta", TINTAS), item("Cano", ENCANAMENTO), item("Vaso", JARDIM),
                item("Cimento", MATERIAIS), item("Espelho", DECORACAO));

        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(TOTEM, itens);

        assertThat(rota).hasSize(5).containsExactlyInAnyOrderElementsOf(itens);
        assertThat(rota.stream().map(ItemRoteiro::getOrdemCaminho).toList())
                .containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("a rota otimizada e mais curta que a ordem em que o cliente adicionou")
    void rotaOtimizadaEncurtaOPercurso() {
        // Ordem de insercao propositalmente ruim: cruza a loja de um lado ao outro.
        List<ItemRoteiro> ordemDeInsercao = List.of(
                item("Tinta", TINTAS),
                item("Espelho", DECORACAO),
                item("Cimento", MATERIAIS),
                item("Cano", ENCANAMENTO),
                item("Vaso", JARDIM));

        double distanciaSemOtimizar = CalculadoraRota.distanciaTotal(TOTEM, ordemDeInsercao);

        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(TOTEM, ordemDeInsercao);
        double distanciaOtimizada = CalculadoraRota.distanciaTotal(TOTEM, rota);

        double reducao = 100 * (distanciaSemOtimizar - distanciaOtimizada) / distanciaSemOtimizar;
        System.out.printf(">>> sem otimizar: %.1f | otimizada: %.1f | reducao: %.1f%%%n",
                distanciaSemOtimizar, distanciaOtimizada, reducao);
        System.out.println(">>> rota: " + rota.stream()
                .map(i -> i.getOrdemCaminho() + ". " + i.getProduto().getPontoMapa().getCorredor())
                .toList());

        assertThat(distanciaOtimizada).isLessThan(distanciaSemOtimizar);
    }

    @Test
    @DisplayName("cenario realista: reforma de banheiro com itens espalhados")
    void cenarioRealista() {
        List<ItemRoteiro> itens = List.of(
                item("Tinta Acrilica", TINTAS),
                item("Cano PVC", ENCANAMENTO),
                item("Cola PVC", ENCANAMENTO),
                item("Argamassa", MATERIAIS),
                item("Espelho", DECORACAO),
                item("Vaso", JARDIM));

        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(TOTEM, itens);

        System.out.println(">>> cenario realista:");
        rota.forEach(i -> System.out.printf("    %d. %-14s (%s)%n",
                i.getOrdemCaminho(),
                i.getProduto().getNome(),
                i.getProduto().getPontoMapa().getCorredor()));
        System.out.printf(">>> distancia total: %.1f%n", CalculadoraRota.distanciaTotal(TOTEM, rota));

        // O primeiro destino tem que ser o mais perto da entrada, e os dois itens de
        // Encanamento nao podem ficar separados na sequencia.
        assertThat(rota.get(0).getProduto().getPontoMapa().getCorredor())
                .isEqualTo("Materiais de construcao");

        List<String> corredores = rota.stream()
                .map(i -> i.getProduto().getPontoMapa().getCorredor())
                .toList();
        assertThat(corredores.indexOf("Encanamento") + 1)
                .isEqualTo(corredores.lastIndexOf("Encanamento"));
    }

    // ---------------------------------------------------------------- refinamento 2-opt

    private static List<String> corredoresDe(List<ItemRoteiro> rota) {
        return rota.stream().map(i -> i.getProduto().getPontoMapa().getCorredor()).toList();
    }

    private static List<ItemRoteiro> itensEm(PontoMapa... pontos) {
        List<ItemRoteiro> itens = new ArrayList<>();
        for (PontoMapa p : pontos) {
            itens.add(item(p.getCorredor(), p));
        }
        return itens;
    }

    @Test
    @DisplayName("desfaz o cruzamento que o vizinho mais proximo deixa para tras")
    void desfazOCruzamento() {
        /*
         * O caso classico em que a heuristica gulosa erra. Da entrada (50,95), Jardim (36,50)
         * e o mais perto, entao o vizinho mais proximo comeca por ele - e depois precisa voltar
         * para o oeste ate Ferramentas (20,55) e so entao atravessar a loja inteira ate
         * Iluminacao (76,32). O caminho se cruza.
         *
         * O 2-opt inverte o trecho: Ferramentas primeiro, Jardim de passagem a caminho do
         * leste, Iluminacao por ultimo. Nenhuma volta.
         */
        List<ItemRoteiro> itens = itensEm(JARDIM, ILUMINACAO, FERRAMENTAS);

        List<ItemRoteiro> guloso = CalculadoraRota.construirPorVizinhoMaisProximo(TOTEM, itens);
        double distanciaGulosa = CalculadoraRota.distanciaTotal(TOTEM, guloso);

        List<ItemRoteiro> refinada = CalculadoraRota.calcularRota(TOTEM, itens);
        double distanciaRefinada = CalculadoraRota.distanciaTotal(TOTEM, refinada);

        System.out.printf(">>> guloso  %.1f -> %s%n", distanciaGulosa, corredoresDe(guloso));
        System.out.printf(">>> 2-opt   %.1f -> %s (reducao de %.1f%%)%n", distanciaRefinada,
                corredoresDe(refinada), 100 * (distanciaGulosa - distanciaRefinada) / distanciaGulosa);

        assertThat(corredoresDe(guloso)).containsExactly("Jardim", "Ferramentas", "Iluminacao");
        assertThat(corredoresDe(refinada)).containsExactly("Ferramentas", "Jardim", "Iluminacao");
        assertThat(distanciaRefinada).isLessThan(distanciaGulosa);
    }

    @Test
    @DisplayName("corrige a travessia desnecessaria num roteiro de cinco corredores")
    void corrigeTravessiaDesnecessaria() {
        // O guloso desce ate Encanamento, vai a Cozinhas, sobe para Iluminacao e volta ao
        // oeste. O 2-opt inverte o inicio e resolve o lado leste de uma vez.
        List<ItemRoteiro> itens = itensEm(ILUMINACAO, FERRAGENS, COZINHAS, ENCANAMENTO, ELETRICA);

        double distanciaGulosa = CalculadoraRota.distanciaTotal(
                TOTEM, CalculadoraRota.construirPorVizinhoMaisProximo(TOTEM, itens));
        double distanciaRefinada = CalculadoraRota.distanciaTotal(
                TOTEM, CalculadoraRota.calcularRota(TOTEM, itens));

        double reducao = 100 * (distanciaGulosa - distanciaRefinada) / distanciaGulosa;
        System.out.printf(">>> cinco corredores: %.1f -> %.1f (reducao de %.1f%%)%n",
                distanciaGulosa, distanciaRefinada, reducao);

        assertThat(reducao).isGreaterThan(10);
    }

    @Test
    @DisplayName("o refinamento nunca piora a rota, em nenhum dos 500 roteiros sorteados")
    void nuncaPioraARota() {
        /*
         * A garantia que de fato importa: o 2-opt so aceita uma inversao que encurte o
         * caminho, entao o resultado e sempre menor ou igual ao do guloso. Uma unica piora
         * aqui significaria erro no calculo do ganho - e o cliente andaria mais por causa da
         * "otimizacao". Semente fixa para o teste ser reproduzivel.
         */
        Random sorteio = new Random(42);
        int melhoraram = 0;
        double somaDosGanhos = 0;
        double maiorGanho = 0;
        final int casos = 500;

        for (int caso = 0; caso < casos; caso++) {
            List<PontoMapa> disponiveis = new ArrayList<>(SECOES);
            Collections.shuffle(disponiveis, sorteio);

            List<ItemRoteiro> itens = new ArrayList<>();
            int quantidade = 3 + sorteio.nextInt(8);
            for (int k = 0; k < quantidade; k++) {
                itens.add(item("P" + k, disponiveis.get(k % disponiveis.size())));
            }

            double guloso = CalculadoraRota.distanciaTotal(
                    TOTEM, CalculadoraRota.construirPorVizinhoMaisProximo(TOTEM, itens));
            double refinado = CalculadoraRota.distanciaTotal(
                    TOTEM, CalculadoraRota.calcularRota(TOTEM, itens));

            assertThat(refinado)
                    .as("roteiro %d: o refinamento fez o cliente andar mais", caso)
                    .isLessThanOrEqualTo(guloso + 1e-6);

            if (refinado < guloso - 1e-6) {
                melhoraram++;
                double ganho = 100 * (guloso - refinado) / guloso;
                somaDosGanhos += ganho;
                maiorGanho = Math.max(maiorGanho, ganho);
            }
        }

        System.out.printf(">>> %d roteiros sorteados: melhorou em %d (%.0f%%), "
                        + "ganho medio quando melhora %.1f%%, maior ganho %.1f%%%n",
                casos, melhoraram, 100.0 * melhoraram / casos,
                somaDosGanhos / melhoraram, maiorGanho);

        assertThat(melhoraram)
                .as("se o 2-opt quase nunca encontrasse ganho, nao valeria o codigo")
                .isGreaterThan(casos / 4);
    }

    @Test
    @DisplayName("o refinamento nao separa itens que dividem o mesmo corredor")
    void refinamentoPreservaOAgrupamento() {
        // Inverter um trecho so compensa quando encurta, e separar itens a distancia zero
        // sempre alonga. Ainda assim, e a propriedade que mais incomodaria o cliente se
        // quebrasse: ele voltaria ao mesmo corredor duas vezes.
        List<ItemRoteiro> itens = itensEm(
                TINTAS, ILUMINACAO, TINTAS, FERRAMENTAS, ILUMINACAO, JARDIM, TINTAS);

        List<String> corredores = corredoresDe(CalculadoraRota.calcularRota(TOTEM, itens));

        System.out.println(">>> agrupamento apos o 2-opt: " + corredores);

        for (String corredor : List.of("Tintas", "Iluminacao")) {
            int primeiro = corredores.indexOf(corredor);
            int ultimo = corredores.lastIndexOf(corredor);
            long ocorrencias = corredores.stream().filter(corredor::equals).count();

            assertThat(ultimo - primeiro + 1)
                    .as("as paradas em %s precisam ser consecutivas", corredor)
                    .isEqualTo((int) ocorrencias);
        }
    }

    @Test
    @DisplayName("com dois itens nao ha o que refinar: o guloso ja escolheu a melhor ordem")
    void doisItensNaoMudam() {
        List<ItemRoteiro> itens = itensEm(DECORACAO, MATERIAIS);

        assertThat(corredoresDe(CalculadoraRota.calcularRota(TOTEM, itens)))
                .isEqualTo(corredoresDe(CalculadoraRota.construirPorVizinhoMaisProximo(TOTEM, itens)));
    }

    @Test
    @DisplayName("a numeracao continua sequencial depois das inversoes")
    void numeracaoSobreviveAoRefinamento() {
        List<ItemRoteiro> rota = CalculadoraRota.calcularRota(
                TOTEM, itensEm(JARDIM, ILUMINACAO, FERRAMENTAS, DECORACAO, TINTAS, ENCANAMENTO));

        assertThat(rota.stream().map(ItemRoteiro::getOrdemCaminho).toList())
                .containsExactly(1, 2, 3, 4, 5, 6);
    }
}
