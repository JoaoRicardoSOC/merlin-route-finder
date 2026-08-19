package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Popula o banco com uma loja Leroy Merlin em miniatura para desenvolvimento e demonstracao.
 * <p>
 * As coordenadas seguem aproximadamente a planta real compartilhada pela Leroy no kickoff,
 * num grid 0-100 (x da esquerda para a direita, y de cima para baixo). Isso importa para a
 * demonstracao: com coordenadas aleatorias a rota calculada nao faria sentido visual quando
 * o mapa fosse desenhado na tela.
 * <p>
 * Roda apenas quando o catalogo esta vazio, entao subir a aplicacao repetidas vezes nao
 * duplica nada. Pode ser desligado com {@code merlin.seed.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "merlin.seed.enabled", havingValue = "true", matchIfMissing = true)
public class CarregadorDadosIniciais implements ApplicationRunner {

    private final ProdutoRepository produtoRepository;
    private final PontoMapaRepository pontoMapaRepository;

    @Override
    public void run(ApplicationArguments args) {
        if (produtoRepository.buscarPaginado(0, 1).totalElementos() > 0) {
            log.info("Catalogo ja possui dados. Carga inicial ignorada.");
            return;
        }

        log.info("Catalogo vazio. Carregando massa de dados de demonstracao...");

        Map<String, PontoMapa> secoes = criarPontosDeVenda();
        criarPontosDeServico();
        criarCatalogo(secoes);

        log.info("Carga inicial concluida: {} secoes e catalogo de demonstracao criados.", secoes.size());
    }

    private Map<String, PontoMapa> criarPontosDeVenda() {
        Map<String, PontoMapa> secoes = new LinkedHashMap<>();
        registrar(secoes, "Tintas", 32, 10);
        registrar(secoes, "Ferragens", 22, 32);
        registrar(secoes, "Eletrica", 34, 30);
        registrar(secoes, "Encanamento", 48, 30);
        registrar(secoes, "Cozinhas", 62, 30);
        registrar(secoes, "Iluminacao", 76, 32);
        registrar(secoes, "Jardim", 36, 50);
        registrar(secoes, "Ferramentas", 20, 55);
        registrar(secoes, "Decoracao", 88, 55);
        registrar(secoes, "Materiais de construcao", 14, 80);
        return secoes;
    }

    private void registrar(Map<String, PontoMapa> secoes, String corredor, int x, int y) {
        secoes.put(corredor, pontoMapaRepository.salvar(
                new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, corredor, x, y)));
    }

    /* Nao vendem nada, mas aparecem na navegacao: o totem e a origem da rota, o caixa o
     * destino final, e o banheiro atende o UC-012 (inclusao de ponto de interesse). */
    private void criarPontosDeServico() {
        pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(), TipoPonto.TOTEM, "Entrada", 50, 95));
        pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(), TipoPonto.CAIXA, "Frente de loja", 62, 88));
        pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(), TipoPonto.BANHEIRO, "Sanitarios", 52, 8));
    }

    private void criarCatalogo(Map<String, PontoMapa> secoes) {
        // Tintas -- concentra o cenario de ruptura de estoque (UC-013): a lixa grao 120 esta
        // zerada e a lixa d'agua grao 150, na mesma secao, e o substituto plausivel que a IA
        // deve encontrar na pre-filtragem espacial.
        produto("SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", "289.90", 12, secoes.get("Tintas"));
        produto("SKU-TIN-002", "Rolo de La 23cm com Cabo", "34.90", 25, secoes.get("Tintas"));
        produto("SKU-TIN-003", "Lixa para Parede Grao 120", "3.50", 0, secoes.get("Tintas"));
        produto("SKU-TIN-004", "Lixa d'Agua Grao 150", "4.20", 40, secoes.get("Tintas"));
        produto("SKU-TIN-005", "Fita Crepe 48mm x 50m", "12.90", 30, secoes.get("Tintas"));

        produto("SKU-FRG-001", "Parafuso Chipboard 4x40mm - 100un", "19.90", 80, secoes.get("Ferragens"));
        produto("SKU-FRG-002", "Bucha de Nylon 8mm - 50un", "15.90", 65, secoes.get("Ferragens"));

        produto("SKU-ELE-001", "Cabo Flexivel 2,5mm 100m", "189.90", 8, secoes.get("Eletrica"));
        produto("SKU-ELE-002", "Interruptor Simples Branco", "14.90", 50, secoes.get("Eletrica"));
        produto("SKU-ELE-003", "Disjuntor Bipolar 25A", "42.90", 15, secoes.get("Eletrica"));

        produto("SKU-ENC-001", "Cano PVC Soldavel 25mm 6m", "28.90", 35, secoes.get("Encanamento"));
        produto("SKU-ENC-002", "Cola para PVC 175g", "18.90", 22, secoes.get("Encanamento"));
        produto("SKU-ENC-003", "Torneira Cromada para Banheiro", "129.90", 10, secoes.get("Encanamento"));
        produto("SKU-ENC-004", "Sifao Sanfonado Universal", "22.50", 18, secoes.get("Encanamento"));

        produto("SKU-COZ-001", "Cuba Inox 56x33cm", "249.90", 6, secoes.get("Cozinhas"));
        produto("SKU-COZ-002", "Torneira Gourmet Cromada", "389.90", 4, secoes.get("Cozinhas"));

        produto("SKU-ILU-001", "Lampada LED 9W Branca - kit 3", "39.90", 60, secoes.get("Iluminacao"));
        produto("SKU-ILU-002", "Luminaria de Embutir Quadrada", "69.90", 14, secoes.get("Iluminacao"));

        produto("SKU-JAR-001", "Vaso de Ceramica 30cm", "79.90", 12, secoes.get("Jardim"));
        produto("SKU-JAR-002", "Terra Vegetal 20kg", "24.90", 30, secoes.get("Jardim"));

        produto("SKU-FER-001", "Furadeira de Impacto 650W", "299.90", 7, secoes.get("Ferramentas"));
        produto("SKU-FER-002", "Trena 5m", "24.90", 45, secoes.get("Ferramentas"));

        produto("SKU-DEC-001", "Espelho Redondo 60cm", "159.90", 9, secoes.get("Decoracao"));

        produto("SKU-MAT-001", "Argamassa AC-II 20kg", "28.90", 50, secoes.get("Materiais de construcao"));
        produto("SKU-MAT-002", "Cimento CP-II 50kg", "42.90", 40, secoes.get("Materiais de construcao"));
    }

    private void produto(String sku, String nome, String preco, int estoque, PontoMapa ponto) {
        produtoRepository.salvar(
                new Produto(UUID.randomUUID(), sku, nome, new BigDecimal(preco), estoque, ponto));
    }
}
