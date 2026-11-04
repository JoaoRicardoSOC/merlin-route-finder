package br.com.jence.backend.seed;

import br.com.jence.backend.model.Produto;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        for (Produto produto : todos()) {
            ProdutoDaMassa declarado = declarados.get(produto.getSku());
            assertNotNull(declarado, "SKU fora da massa: " + produto.getSku());
            assertEquals(declarado.nome(), produto.getNome(),
                    "nome gravado de " + produto.getSku());
            assertEquals(declarado.descricao(), produto.getDescricao(),
                    "descricao gravada de " + produto.getSku());
        }
    }

    @Test
    @DisplayName("mexer no que esta gravado e recarregar restaura o que a massa diz")
    void aCargaDesfazUmaAlteracaoFeitaAMao() {
        Produto antes = porSku("SKU-DEC-001");

        produtoRepository.salvar(antes.comApresentacao(
                "Nome trocado a mao", "Descricao trocada a mao", null));
        assertEquals("Nome trocado a mao", porSku("SKU-DEC-001").getNome());

        carregador.run(null);

        Produto depois = porSku("SKU-DEC-001");
        assertEquals(antes.getNome(), depois.getNome());
        assertEquals(antes.getDescricao(), depois.getDescricao());
        assertEquals(antes.getImagemUrl(), depois.getImagemUrl(),
                "a foto tambem volta - foi apagada junto");
    }

    @Test
    @DisplayName("o saldo em estoque tambem volta a ser o que a massa declara")
    void oEstoqueVoltaAoDeclarado() {
        /*
         * Estoque ficou de fora da reconciliacao por muito tempo, e so apareceu quando a lixa
         * da ruptura passou a nascer com saldo (D-72): a mudanca nao chegava a banco nenhum
         * que ja tivesse o produto - ou seja, a lugar nenhum, ja que o schema e um so.
         */
        Produto antes = porSku("SKU-TIN-003");
        int declarado = antes.getSaldoEstoque();

        produtoRepository.salvar(antes.comSaldoEstoque(declarado + 7));
        assertEquals(declarado + 7, porSku("SKU-TIN-003").getSaldoEstoque());

        carregador.run(null);

        assertEquals(declarado, porSku("SKU-TIN-003").getSaldoEstoque(),
                "a carga precisa devolver o estoque ensaiado");
    }

    @Test
    @DisplayName("nome e imagem cabem nas colunas")
    void cabemNasColunas() {
        // Um estouro aqui nao falha em teste: falha na carga, e so no banco de quem rodar.
        for (Produto produto : todos()) {
            assertTrue(produto.getNome().length() <= 200, "nome de " + produto.getSku());
            if (produto.getImagemUrl() != null) {
                assertTrue(produto.getImagemUrl().length() <= 500, "imagem de " + produto.getSku());
                assertTrue(produto.getImagemUrl().startsWith("https://"));
            }
        }
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
        assertFalse(comFoto.isEmpty(), "nenhuma das URLs coletadas chegou aos produtos");
    }
}
