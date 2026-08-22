package br.com.jence.backend;

import br.com.jence.backend.application.usecase.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A consulta espacial da ruptura contra o Oracle real. Exige banco, nao exige GEMINI_API_KEY:
 * o objetivo aqui e provar a pre-filtragem sozinha, sem gastar cota do provedor de IA.
 */
@Tag("integracao")
@SpringBootTest
class PreFiltragemEspacialIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;

    private Produto lixaEmFalta() {
        return produtoRepository.buscarPorSku("SKU-TIN-003").orElseThrow();
    }

    private List<Produto> candidatos() {
        Produto emFalta = lixaEmFalta();
        return produtoRepository.buscarDisponiveisProximosDe(
                emFalta.getPontoMapa(),
                emFalta.getId(),
                TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA,
                TratarRupturaEstoqueUseCase.LIMITE_DE_CANDIDATOS);
    }

    @Test
    void devolveVizinhosDisponiveisEmOrdemDeDistancia() {
        Produto emFalta = lixaEmFalta();
        List<Produto> proximos = candidatos();

        System.out.println(">>> em falta: " + emFalta.getNome()
                + " (" + emFalta.getPontoMapa().getCorredor() + ")");
        proximos.forEach(p -> System.out.printf("    %5.1f  %-40s %s%n",
                emFalta.getPontoMapa().calcularDistanciaPara(p.getPontoMapa()),
                p.getNome(), p.getPontoMapa().getCorredor()));

        assertThat(proximos).isNotEmpty();

        assertThat(proximos)
                .as("o produto em falta nao pode substituir a si mesmo")
                .noneMatch(p -> p.getId().equals(emFalta.getId()));

        assertThat(proximos)
                .as("candidato sem estoque nao ajuda quem ja esta diante de uma prateleira vazia")
                .allMatch(Produto::temDisponibilidade);

        assertThat(proximos)
                .as("a ordenacao por distancia acontece no banco")
                .isSortedAccordingTo((a, b) -> Double.compare(
                        emFalta.getPontoMapa().calcularDistanciaPara(a.getPontoMapa()),
                        emFalta.getPontoMapa().calcularDistanciaPara(b.getPontoMapa())));

        assertThat(proximos)
                .as("o raio precisa alcancar o substituto plantado na massa de demonstracao")
                .anyMatch(p -> p.getSku().equals("SKU-TIN-004"));
    }

    @Test
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
    void respeitaOTetoDeCandidatos() {
        Produto emFalta = lixaEmFalta();

        List<Produto> apenasDois = produtoRepository.buscarDisponiveisProximosDe(
                emFalta.getPontoMapa(), emFalta.getId(), TratarRupturaEstoqueUseCase.RAIO_DE_BUSCA, 2);

        assertThat(apenasDois).hasSize(2);
    }
}
