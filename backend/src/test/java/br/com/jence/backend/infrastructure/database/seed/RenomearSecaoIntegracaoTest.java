package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.BlocoMapa;
import br.com.jence.backend.domain.entity.PlantaDaLoja;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.PontoMapaJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Renomear uma secao e migracao de dado, nao edicao de string (D-70).
 * <p>
 * A carga casa secao existente <b>pelo nome do corredor</b>. Sem o passo de renomeacao, mudar
 * o nome na {@link PlantaDaLoja} faria a carga criar um ponto novo e vazio, e os produtos
 * ficariam presos ao ponto de nome velho: a secao apareceria duas vezes no mapa, uma com todos
 * os produtos e outra com nenhum.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class RenomearSecaoIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;
    @Autowired PontoMapaRepository pontoMapaRepository;
    @Autowired PontoMapaJpaRepository pontoMapaJpaRepository;
    @Autowired CarregadorDadosIniciais carregador;

    private List<PontoMapa> prateleiras() {
        return pontoMapaRepository.buscarPorTipo(TipoPonto.PRATELEIRA);
    }

    private PontoMapa porCorredor(String corredor) {
        return prateleiras().stream()
                .filter(ponto -> ponto.getCorredor().equals(corredor))
                .findFirst()
                .orElseThrow(() -> new AssertionError("secao ausente no mapa: " + corredor));
    }

    @Test
    @DisplayName("cada secao da planta aparece uma vez so, com o nome acentuado")
    void cadaSecaoUmaVezSo() {
        Map<String, Long> porNome = prateleiras().stream()
                .collect(Collectors.groupingBy(PontoMapa::getCorredor, Collectors.counting()));

        System.out.println(">>> secoes gravadas: " + porNome.keySet());

        assertThat(porNome.values())
                .as("secao repetida dividiria os produtos de um corredor em dois pontos")
                .allMatch(quantidade -> quantidade == 1);

        assertThat(porNome.keySet())
                .as("o que esta gravado tem que ser exatamente o que a planta declara")
                .containsExactlyInAnyOrderElementsOf(
                        PlantaDaLoja.blocos().stream().map(BlocoMapa::rotulo).toList());

        assertThat(porNome.keySet())
                .as("os nomes sem acento sao os antigos e nao podem ter sobrado")
                .doesNotContain("Eletrica", "Iluminacao", "Decoracao", "Materiais de construcao");
    }

    @Test
    @DisplayName("renomear preserva o ponto e os produtos que apontam para ele")
    void renomearPreservaOsProdutos() {
        /*
         * A prova direta do que D-70 protege: volta o nome antigo em banco, roda a carga e
         * verifica que o ponto e o mesmo - mesmo id, mesmas coordenadas - e que os produtos
         * continuam nele. Se a carga tivesse criado um ponto novo, o id mudaria e a contagem
         * de produtos da secao cairia para zero.
         */
        PontoMapa antes = porCorredor("Decoração");
        UUID idOriginal = antes.getId();
        long produtosAntes = produtosEm("Decoração");

        assertThat(produtosAntes).as("a secao precisa ter produtos para o teste valer").isPositive();

        pontoMapaJpaRepository.renomearCorredor("Decoração", "Decoracao");
        assertThat(porCorredor("Decoracao").getId()).isEqualTo(idOriginal);

        carregador.run(null);

        PontoMapa depois = porCorredor("Decoração");
        assertThat(depois.getId())
                .as("a carga criou um ponto novo em vez de renomear o que ja existia")
                .isEqualTo(idOriginal);
        assertThat(depois.getCoordenadaX()).isEqualTo(antes.getCoordenadaX());
        assertThat(depois.getCoordenadaY()).isEqualTo(antes.getCoordenadaY());
        assertThat(produtosEm("Decoração")).isEqualTo(produtosAntes);

        assertThat(prateleiras())
                .as("o nome antigo nao pode ter sobrado como ponto orfao")
                .noneMatch(ponto -> ponto.getCorredor().equals("Decoracao"));
    }

    private long produtosEm(String corredor) {
        return produtoRepository.buscarPaginado(0, 1000).conteudo().stream()
                .map(Produto::getPontoMapa)
                .filter(ponto -> ponto != null && corredor.equals(ponto.getCorredor()))
                .count();
    }
}
