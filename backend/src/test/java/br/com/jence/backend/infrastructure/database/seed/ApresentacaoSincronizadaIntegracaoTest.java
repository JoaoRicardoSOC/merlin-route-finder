package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A massa e a fonte da apresentacao, e o que esta gravado tem que segui-la (D-69).
 * <p>
 * Vive neste pacote de proposito: le {@link CatalogoDaMassa} diretamente, e tornar a classe
 * publica so para um teste enxerga-la seria alargar a producao por conveniencia de teste.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class ApresentacaoSincronizadaIntegracaoTest {

    @Autowired ProdutoRepository produtoRepository;
    @Autowired CarregadorDadosIniciais carregador;

    private List<Produto> todos() {
        return produtoRepository.buscarPaginado(0, 1000).conteudo();
    }

    private Produto porSku(String sku) {
        return produtoRepository.buscarPorSku(sku).orElseThrow(
                () -> new AssertionError("produto ausente na massa: " + sku));
    }

    @Test
    @DisplayName("nome e descricao gravados sao os que a massa declara, em todos os produtos")
    void oGravadoBateComAMassa() {
        /*
         * Este teste existe porque a regra mudou.
         *
         * Enquanto a carga so preenchia campo vazio, corrigir um nome no codigo nao chegava a
         * banco nenhum que ja tivesse o produto - inclusive o publicado, que e o que a banca
         * ve. Foi o que quase aconteceu quando os nomes reais do site entraram junto das
         * fotos: o atributo MARCA sincroniza sempre e o nome nao sincronizava nunca, e a ficha
         * tecnica passaria a dizer Gavix embaixo de um nome que dizia outra coisa.
         */
        Map<String, ProdutoDaMassa> declarados = CatalogoDaMassa.produtos().stream()
                .collect(Collectors.toMap(ProdutoDaMassa::sku, Function.identity()));

        assertThat(todos()).allSatisfy(produto -> {
            ProdutoDaMassa declarado = declarados.get(produto.getSku());
            assertThat(declarado).as("SKU fora da massa: %s", produto.getSku()).isNotNull();
            assertThat(produto.getNome())
                    .as("nome gravado de %s", produto.getSku()).isEqualTo(declarado.nome());
            assertThat(produto.getDescricao())
                    .as("descricao gravada de %s", produto.getSku()).isEqualTo(declarado.descricao());
        });
    }

    @Test
    @DisplayName("mexer no que esta gravado e recarregar restaura o que a massa diz")
    void aCargaDesfazUmaAlteracaoFeitaAMao() {
        Produto antes = porSku("SKU-DEC-001");

        produtoRepository.salvar(antes.comApresentacao(
                "Nome trocado a mao", "Descricao trocada a mao", null));
        assertThat(porSku("SKU-DEC-001").getNome()).isEqualTo("Nome trocado a mao");

        carregador.run(null);

        Produto depois = porSku("SKU-DEC-001");
        assertThat(depois.getNome()).isEqualTo(antes.getNome());
        assertThat(depois.getDescricao()).isEqualTo(antes.getDescricao());
        assertThat(depois.getImagemUrl())
                .as("a foto tambem volta - foi apagada junto")
                .isEqualTo(antes.getImagemUrl());
    }

    @Test
    @DisplayName("nome e imagem cabem nas colunas")
    void cabemNasColunas() {
        // Um estouro aqui nao falha em teste: falha na carga, e so no banco de quem rodar.
        assertThat(todos()).allSatisfy(produto -> {
            assertThat(produto.getNome().length())
                    .as("nome de %s", produto.getSku()).isLessThanOrEqualTo(200);
            if (produto.getImagemUrl() != null) {
                assertThat(produto.getImagemUrl().length())
                        .as("imagem de %s", produto.getSku()).isLessThanOrEqualTo(500);
                assertThat(produto.getImagemUrl()).startsWith("https://");
            }
        });
    }

    @Test
    @DisplayName("as fotos coletadas chegaram aos produtos")
    void asFotosColetadasChegaram() {
        List<Produto> comFoto = todos().stream()
                .filter(produto -> produto.getImagemUrl() != null)
                .toList();

        System.out.printf(">>> produtos com foto: %d de %d%n", comFoto.size(), todos().size());

        // Imagem nula continua sendo estado normal (O-18); o que se prova aqui e que a coleta
        // do time chegou ao banco, e nao ficou so no mapa do codigo.
        assertThat(comFoto).as("nenhuma das URLs coletadas chegou aos produtos").isNotEmpty();
    }
}
