package br.com.jence.backend;

import br.com.jence.backend.application.usecase.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;
import br.com.jence.backend.domain.repository.AfinidadeDeProduto;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A consulta que escolhe os candidatos a substituto, contra o Oracle real.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY - e essa e a graca: o que se prova aqui e a lista que
 * o assistente recebe e, quando ele esta fora do ar, <b>a propria resposta dada ao cliente</b>.
 */
@Tag("integracao")
@SpringBootTest
class PreFiltragemEspacialIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;

    /** Os pares plantados na massa: produto em falta e o substituto que faz sentido. */
    private static final Map<String, String> PARES = Map.of(
            "SKU-TIN-003", "SKU-TIN-004",   // lixa grao 120   -> lixa d'agua grao 150
            "SKU-ILU-001", "SKU-ILU-003",   // lampada 9W      -> lampada 12W
            "SKU-ENC-004", "SKU-ENC-005",   // sifao sanfonado -> sifao copo
            "SKU-FER-002", "SKU-FER-003",   // trena 5m        -> trena 7,5m
            "SKU-MAT-001", "SKU-MAT-003");  // argamassa AC-II -> argamassa AC-III

    private Produto porSku(String sku) {
        return produtoRepository.buscarPorSku(sku).orElseThrow();
    }

    private Produto lixaEmFalta() {
        return porSku("SKU-TIN-003");
    }

    private List<Produto> candidatosPara(Produto emFalta, int limite) {
        return produtoRepository.buscarDisponiveisProximosDe(
                emFalta.getPontoMapa(),
                emFalta.getId(),
                AfinidadeDeProduto.de(produtoRepository.buscarAtributosDe(emFalta.getId())),
                TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA,
                limite);
    }

    private List<Produto> candidatos() {
        return candidatosPara(lixaEmFalta(), TratarRupturaEstoqueUseCase.LIMITE_DE_CANDIDATOS);
    }

    private String tipoDe(Produto produto) {
        return produtoRepository.buscarAtributosDe(produto.getId()).stream()
                .filter(atributo -> atributo.atributo() == AtributoProduto.TIPO)
                .map(ValorDeAtributo::valor)
                .findFirst()
                .orElse(null);
    }

    // ---------------------------------------------------------------- o filtro espacial

    @Test
    @DisplayName("devolve vizinhos disponiveis, sem o proprio produto em falta")
    void devolveVizinhosDisponiveis() {
        Produto emFalta = lixaEmFalta();
        List<Produto> proximos = candidatos();

        System.out.println(">>> em falta: " + emFalta.getNome()
                + " (" + emFalta.getPontoMapa().getCorredor() + ")");
        proximos.forEach(p -> System.out.printf("    %5.1f  %-22s %-40s %s%n",
                emFalta.getPontoMapa().calcularDistanciaPara(p.getPontoMapa()),
                tipoDe(p), p.getNome(), p.getPontoMapa().getCorredor()));

        assertThat(proximos).isNotEmpty();

        assertThat(proximos)
                .as("o produto em falta nao pode substituir a si mesmo")
                .noneMatch(p -> p.getId().equals(emFalta.getId()));

        assertThat(proximos)
                .as("candidato sem estoque nao ajuda quem ja esta diante de uma prateleira vazia")
                .allMatch(Produto::temDisponibilidade);
    }

    @Test
    @DisplayName("o raio exclui o outro extremo da loja")
    void oRaioExcluiOOutroExtremoDaLoja() {
        Produto emFalta = lixaEmFalta();

        assertThat(candidatos())
                .as("Tintas fica em (32,10) e Materiais de construcao em (14,80): fora do raio")
                .noneMatch(p -> p.getPontoMapa().getCorredor().equals("Materiais de construcao"))
                .allSatisfy(p -> assertThat(
                        emFalta.getPontoMapa().calcularDistanciaPara(p.getPontoMapa()))
                        .isLessThanOrEqualTo(TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA));
    }

    @Test
    @DisplayName("respeita o teto de candidatos")
    void respeitaOTetoDeCandidatos() {
        assertThat(candidatosPara(lixaEmFalta(), 2)).hasSize(2);
    }

    @Test
    @DisplayName("o teto precisa alcancar outro corredor, e nao so a propria secao")
    void oTetoAlcancaOutroCorredor() {
        /*
         * A regressao que o catalogo maior revelou, e a razao de o teto ter subido para 20.
         *
         * Todos os produtos de uma secao compartilham a coordenada do bloco (D-58), entao
         * empatam em distancia. Com uma dezena de produtos por corredor, um teto baixo NUNCA
         * sai do corredor atual - e a pre-filtragem deixa de oferecer o que esta perto para
         * oferecer o que esta ao lado na prateleira, que e outra coisa.
         */
        List<String> corredores = candidatos().stream()
                .map(produto -> produto.getPontoMapa().getCorredor())
                .distinct()
                .toList();

        System.out.println(">>> corredores alcancados: " + corredores);

        assertThat(corredores)
                .as("com o teto atual, os candidatos ficaram presos a uma unica secao")
                .hasSizeGreaterThan(1);
    }

    // ---------------------------------------------------------------- afinidade antes de distancia

    @Test
    @DisplayName("o primeiro candidato e o substituto plantado, em todos os pares da massa")
    void oPrimeiroCandidatoEOSubstitutoCerto() {
        /*
         * ESTE e o teste que importa, e ele nasceu de um defeito real.
         *
         * O primeiro candidato e o que o cliente recebe quando o assistente esta fora do ar ou
         * a cota estourou (D-35) - o cenario mais provavel de acontecer durante a banca, porque
         * o tier gratuito permite cinco chamadas por minuto (O-01).
         *
         * Antes da afinidade, com o catalogo de 111 produtos, esta lista dava: bandeja de
         * pintura para uma lixa, alicate para uma trena, areia para argamassa. Cinco de cinco
         * errados. Como todos os produtos de uma secao empatam em distancia, o desempate caia
         * no nome, e "o mais proximo" virou "o primeiro do corredor em ordem alfabetica".
         */
        PARES.forEach((skuEmFalta, skuEsperado) -> {
            Produto emFalta = porSku(skuEmFalta);
            Produto primeiro = candidatosPara(
                    emFalta, TratarRupturaEstoqueUseCase.LIMITE_DE_CANDIDATOS).getFirst();

            System.out.printf(">>> %-14s %-34s -> %s%n",
                    skuEmFalta, emFalta.getNome(), primeiro.getNome());

            assertThat(primeiro.getSku())
                    .as("substituto oferecido para '%s' quando a IA esta fora do ar", emFalta.getNome())
                    .isEqualTo(skuEsperado);
        });
    }

    @Test
    @DisplayName("candidatos do mesmo tipo vem antes dos demais")
    void mesmoTipoPrimeiro() {
        Produto emFalta = lixaEmFalta();
        String tipoEmFalta = tipoDe(emFalta);

        assertThat(tipoEmFalta).isNotNull();

        List<Produto> proximos = candidatos();
        int ultimoDoMesmoTipo = -1;
        int primeiroDeOutroTipo = proximos.size();

        for (int i = 0; i < proximos.size(); i++) {
            if (tipoEmFalta.equals(tipoDe(proximos.get(i)))) {
                ultimoDoMesmoTipo = i;
            } else if (primeiroDeOutroTipo == proximos.size()) {
                primeiroDeOutroTipo = i;
            }
        }

        assertThat(ultimoDoMesmoTipo)
                .as("um produto de outro tipo apareceu antes de um do mesmo tipo")
                .isLessThan(primeiroDeOutroTipo);
    }

    @Test
    @DisplayName("sem afinidade conhecida, a ordem cai de volta para a distancia")
    void semAfinidadeOrdenaPorDistancia() {
        /*
         * Um produto sem TIPO nem MARCA nao deve quebrar a consulta: os dois CASE devolvem 1
         * para todo mundo e a ordenacao volta a ser a de antes. E o que garante que a massa
         * poder ficar incompleta nao vira um erro em producao.
         */
        Produto emFalta = lixaEmFalta();

        List<Produto> semAfinidade = produtoRepository.buscarDisponiveisProximosDe(
                emFalta.getPontoMapa(), emFalta.getId(), AfinidadeDeProduto.nenhuma(),
                TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA,
                TratarRupturaEstoqueUseCase.LIMITE_DE_CANDIDATOS);

        assertThat(semAfinidade).isNotEmpty();
        assertThat(semAfinidade)
                .isSortedAccordingTo((a, b) -> Double.compare(
                        emFalta.getPontoMapa().calcularDistanciaPara(a.getPontoMapa()),
                        emFalta.getPontoMapa().calcularDistanciaPara(b.getPontoMapa())));
    }
}
