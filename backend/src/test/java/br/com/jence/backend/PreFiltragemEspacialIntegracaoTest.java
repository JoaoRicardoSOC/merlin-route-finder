package br.com.jence.backend;

import br.com.jence.backend.service.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.ValorDeAtributo;
import br.com.jence.backend.model.AfinidadeDeProduto;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            "SKU-ILU-001", "SKU-ILU-005",   // lampada branca  -> outra lampada branca
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

        assertFalse(proximos.isEmpty());

        assertTrue(proximos.stream().noneMatch(p -> p.getId().equals(emFalta.getId())),
                "o produto em falta nao pode substituir a si mesmo");

        assertTrue(proximos.stream().allMatch(Produto::temDisponibilidade),
                "candidato sem estoque nao ajuda quem ja esta diante de uma prateleira vazia");
    }

    @Test
    @DisplayName("o raio exclui o outro extremo da loja")
    void oRaioExcluiOOutroExtremoDaLoja() {
        Produto emFalta = lixaEmFalta();

        List<Produto> candidatos = candidatos();
        String mensagem = "Tintas fica em (32,10) e Materiais de construção em (14,80): fora do raio";

        assertTrue(candidatos.stream()
                .noneMatch(p -> p.getPontoMapa().getCorredor().equals("Materiais de construção")),
                mensagem);
        for (Produto p : candidatos) {
            assertTrue(emFalta.getPontoMapa().calcularDistanciaPara(p.getPontoMapa())
                    <= TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA, mensagem);
        }
    }

    @Test
    @DisplayName("respeita o teto de candidatos")
    void respeitaOTetoDeCandidatos() {
        assertEquals(2, candidatosPara(lixaEmFalta(), 2).size());
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

        assertTrue(corredores.size() > 1,
                "com o teto atual, os candidatos ficaram presos a uma unica secao");
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

            assertEquals(skuEsperado, primeiro.getSku(),
                    String.format("substituto oferecido para '%s' quando a IA esta fora do ar",
                            emFalta.getNome()));
        });
    }

    @Test
    @DisplayName("candidatos do mesmo tipo vem antes dos demais")
    void mesmoTipoPrimeiro() {
        Produto emFalta = lixaEmFalta();
        String tipoEmFalta = tipoDe(emFalta);

        assertNotNull(tipoEmFalta);

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

        assertTrue(ultimoDoMesmoTipo < primeiroDeOutroTipo,
                "um produto de outro tipo apareceu antes de um do mesmo tipo");
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

        assertFalse(semAfinidade.isEmpty());
        // ordenada pela distancia: igual a uma copia ordenada (o sort e estavel)
        Comparator<Produto> porDistancia = (a, b) -> Double.compare(
                emFalta.getPontoMapa().calcularDistanciaPara(a.getPontoMapa()),
                emFalta.getPontoMapa().calcularDistanciaPara(b.getPontoMapa()));
        assertEquals(semAfinidade.stream().sorted(porDistancia).toList(), semAfinidade);
    }
}
