package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.model.BlocoMapa;
import br.com.jence.backend.model.PlantaDaLoja;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        assertTrue(porNome.values().stream().allMatch(quantidade -> quantidade == 1),
                "secao repetida dividiria os produtos de um corredor em dois pontos");

        // Comparacao como multiconjunto: copias ordenadas, que tambem falham se os tamanhos diferirem.
        assertEquals(
                PlantaDaLoja.blocos().stream().map(BlocoMapa::rotulo).sorted().toList(),
                porNome.keySet().stream().sorted().toList(),
                "o que esta gravado tem que ser exatamente o que a planta declara");

        for (String antigo : List.of("Eletrica", "Iluminacao", "Decoracao", "Materiais de construcao")) {
            assertFalse(porNome.keySet().contains(antigo),
                    "os nomes sem acento sao os antigos e nao podem ter sobrado");
        }
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

        assertTrue(produtosAntes > 0, "a secao precisa ter produtos para o teste valer");

        pontoMapaJpaRepository.renomearCorredor("Decoração", "Decoracao");
        assertEquals(idOriginal, porCorredor("Decoracao").getId());

        carregador.run(null);

        PontoMapa depois = porCorredor("Decoração");
        assertEquals(idOriginal, depois.getId(),
                "a carga criou um ponto novo em vez de renomear o que ja existia");
        assertEquals(antes.getCoordenadaX(), depois.getCoordenadaX());
        assertEquals(antes.getCoordenadaY(), depois.getCoordenadaY());
        assertEquals(produtosAntes, produtosEm("Decoração"));

        assertTrue(prateleiras().stream().noneMatch(ponto -> ponto.getCorredor().equals("Decoracao")),
                "o nome antigo nao pode ter sobrado como ponto orfao");
    }

    private long produtosEm(String corredor) {
        return produtoRepository.buscarPaginado(0, 1000).conteudo().stream()
                .map(Produto::getPontoMapa)
                .filter(ponto -> ponto != null && corredor.equals(ponto.getCorredor()))
                .count();
    }
}
