package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
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
}
