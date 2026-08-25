package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.PontoMapaJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Popula o banco com uma loja Leroy Merlin em miniatura para desenvolvimento e demonstracao.
 * <p>
 * As coordenadas seguem aproximadamente a planta real compartilhada pela Leroy no kickoff,
 * num grid 0-100 (x da esquerda para a direita, y de cima para baixo). Isso importa para a
 * demonstracao: e sobre esse grid que o mapa e desenhado, e com coordenadas aleatorias os
 * produtos apareceriam em lugares que nao correspondem a loja.
 * <p>
 * <b>A carga e incremental</b>, nao tudo-ou-nada: cada secao e cada produto so e criado se
 * ainda nao existir. Assim um produto novo acrescentado aqui chega tambem aos bancos que ja
 * tinham a massa antiga - inclusive o da instancia publicada. Ver D-47.
 * <p>
 * Pode ser desligado com {@code merlin.seed.enabled=false}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "merlin.seed.enabled", havingValue = "true", matchIfMissing = true)
public class CarregadorDadosIniciais implements ApplicationRunner {

    /*
     * Teto da leitura que descobre o que ja existe. Precisa ser maior que o catalogo de
     * demonstracao com folga; se um dia for ultrapassado, a carga se recusa a rodar em vez de
     * arriscar inserir SKU duplicado.
     */
    private static final int LIMITE_DE_LEITURA = 1000;

    /*
     * Tipos de ponto que existiram no banco e sairam do enum. Enquanto a linha continuar la,
     * qualquer leitura que traga todos os pontos - a planta da loja, por exemplo - quebra na
     * conversao para TipoPonto. Como a carga e incremental e nunca apaga nada, este e o unico
     * lugar que pode limpar a massa de quem ja rodou a versao anterior.
     */
    private static final List<String> TIPOS_APOSENTADOS = List.of("TOTEM");

    private final ProdutoRepository produtoRepository;
    private final PontoMapaRepository pontoMapaRepository;
    private final PontoMapaJpaRepository pontoMapaJpaRepository;

    /*
     * O que esta execucao criou. Vive numa instancia propria por chamada, e nao em campo do
     * componente: como ele e um singleton do Spring, contadores de instancia se somariam entre
     * execucoes e o log passaria a mentir a partir da segunda - foi o que um teste flagrou.
     */
    private static final class Contagem {
        private int secoes;
        private int produtos;

        private boolean nadaFeito() {
            return secoes == 0 && produtos == 0;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        Pagina<Produto> existentes = produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA);

        if (existentes.totalElementos() > existentes.conteudo().size()) {
            log.warn("Catalogo tem {} produtos, acima do limite de leitura da carga inicial ({}). "
                            + "Carga ignorada para nao arriscar duplicar SKU.",
                    existentes.totalElementos(), LIMITE_DE_LEITURA);
            return;
        }

        Set<String> skusExistentes = existentes.conteudo().stream()
                .map(Produto::getSku)
                .collect(Collectors.toSet());

        Contagem contagem = new Contagem();
        apagarPontosDeTipoAposentado();
        Map<String, PontoMapa> secoes = carregarOuCriarSecoes(contagem);
        criarPontosDeServicoQueFaltam(contagem);
        criarCatalogo(secoes, skusExistentes, contagem);

        if (contagem.nadaFeito()) {
            log.info("Massa de demonstracao ja esta completa. Nada a carregar.");
        } else {
            log.info("Carga incremental: {} secao(oes) e {} produto(s) criados.",
                    contagem.secoes, contagem.produtos);
        }
    }

    // ---------------------------------------------------------------- pontos do mapa

    private void apagarPontosDeTipoAposentado() {
        for (String tipo : TIPOS_APOSENTADOS) {
            int apagados = pontoMapaJpaRepository.apagarPorTipoBruto(tipo);
            if (apagados > 0) {
                log.info("Ponto de tipo {}, aposentado pelo escopo revisado: {} linha(s) apagada(s).",
                        tipo, apagados);
            }
        }
    }

    private Map<String, PontoMapa> carregarOuCriarSecoes(Contagem contagem) {
        Map<String, PontoMapa> existentes = pontoMapaRepository.buscarPorTipo(TipoPonto.PRATELEIRA)
                .stream()
                .collect(Collectors.toMap(PontoMapa::getCorredor, Function.identity(), (a, b) -> a));

        Map<String, PontoMapa> secoes = new LinkedHashMap<>();
        registrar(secoes, existentes, contagem, "Tintas", 32, 10);
        registrar(secoes, existentes, contagem, "Ferragens", 22, 32);
        registrar(secoes, existentes, contagem, "Eletrica", 34, 30);
        registrar(secoes, existentes, contagem, "Encanamento", 48, 30);
        registrar(secoes, existentes, contagem, "Cozinhas", 62, 30);
        registrar(secoes, existentes, contagem, "Iluminacao", 76, 32);
        registrar(secoes, existentes, contagem, "Jardim", 36, 50);
        registrar(secoes, existentes, contagem, "Ferramentas", 20, 55);
        registrar(secoes, existentes, contagem, "Decoracao", 88, 55);
        registrar(secoes, existentes, contagem, "Materiais de construcao", 14, 80);
        return secoes;
    }

    private void registrar(Map<String, PontoMapa> secoes, Map<String, PontoMapa> existentes,
                           Contagem contagem, String corredor, int x, int y) {
        PontoMapa ponto = existentes.get(corredor);
        if (ponto == null) {
            ponto = pontoMapaRepository.salvar(
                    new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, corredor, x, y));
            contagem.secoes++;
        }
        secoes.put(corredor, ponto);
    }

    /* Nao vendem nada, mas precisam aparecer no mapa: o cliente vai ate os caixas para
     * fechar a compra, e o banheiro e uma parada que ele pode querer localizar. */
    private void criarPontosDeServicoQueFaltam(Contagem contagem) {
        criarSeNaoHouver(contagem, TipoPonto.CAIXA, "Frente de loja", 62, 88);
        criarSeNaoHouver(contagem, TipoPonto.BANHEIRO, "Sanitarios", 52, 8);
    }

    private void criarSeNaoHouver(Contagem contagem, TipoPonto tipo, String corredor, int x, int y) {
        if (pontoMapaRepository.buscarPorTipo(tipo).isEmpty()) {
            pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(), tipo, corredor, x, y));
            contagem.secoes++;
        }
    }

    // ---------------------------------------------------------------- catalogo

    private void criarCatalogo(Map<String, PontoMapa> secoes, Set<String> jaExistentes,
                               Contagem contagem) {
        /*
         * Tintas concentra o cenario de ruptura de estoque (UC-013): a lixa grao 120 esta
         * zerada e a lixa d'agua grao 150, na mesma secao, e o substituto plausivel que a IA
         * deve encontrar na pre-filtragem espacial.
         */
        produto(jaExistentes, contagem, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", "289.90", 12, secoes.get("Tintas"));
        produto(jaExistentes, contagem, "SKU-TIN-002", "Rolo de La 23cm com Cabo", "34.90", 25, secoes.get("Tintas"));
        produto(jaExistentes, contagem, "SKU-TIN-003", "Lixa para Parede Grao 120", "3.50", 0, secoes.get("Tintas"));
        produto(jaExistentes, contagem, "SKU-TIN-004", "Lixa d'Agua Grao 150", "4.20", 40, secoes.get("Tintas"));
        produto(jaExistentes, contagem, "SKU-TIN-005", "Fita Crepe 48mm x 50m", "12.90", 30, secoes.get("Tintas"));

        produto(jaExistentes, contagem, "SKU-FRG-001", "Parafuso Chipboard 4x40mm - 100un", "19.90", 80, secoes.get("Ferragens"));
        produto(jaExistentes, contagem, "SKU-FRG-002", "Bucha de Nylon 8mm - 50un", "15.90", 65, secoes.get("Ferragens"));

        produto(jaExistentes, contagem, "SKU-ELE-001", "Cabo Flexivel 2,5mm 100m", "189.90", 8, secoes.get("Eletrica"));
        produto(jaExistentes, contagem, "SKU-ELE-002", "Interruptor Simples Branco", "14.90", 50, secoes.get("Eletrica"));
        produto(jaExistentes, contagem, "SKU-ELE-003", "Disjuntor Bipolar 25A", "42.90", 15, secoes.get("Eletrica"));

        produto(jaExistentes, contagem, "SKU-ENC-001", "Cano PVC Soldavel 25mm 6m", "28.90", 35, secoes.get("Encanamento"));
        produto(jaExistentes, contagem, "SKU-ENC-002", "Cola para PVC 175g", "18.90", 22, secoes.get("Encanamento"));
        produto(jaExistentes, contagem, "SKU-ENC-003", "Torneira Cromada para Banheiro", "129.90", 10, secoes.get("Encanamento"));
        produto(jaExistentes, contagem, "SKU-ENC-004", "Sifao Sanfonado Universal", "22.50", 18, secoes.get("Encanamento"));

        produto(jaExistentes, contagem, "SKU-COZ-001", "Cuba Inox 56x33cm", "249.90", 6, secoes.get("Cozinhas"));
        produto(jaExistentes, contagem, "SKU-COZ-002", "Torneira Gourmet Cromada", "389.90", 4, secoes.get("Cozinhas"));

        produto(jaExistentes, contagem, "SKU-ILU-001", "Lampada LED 9W Branca - kit 3", "39.90", 60, secoes.get("Iluminacao"));
        produto(jaExistentes, contagem, "SKU-ILU-002", "Luminaria de Embutir Quadrada", "69.90", 14, secoes.get("Iluminacao"));

        produto(jaExistentes, contagem, "SKU-JAR-001", "Vaso de Ceramica 30cm", "79.90", 12, secoes.get("Jardim"));
        produto(jaExistentes, contagem, "SKU-JAR-002", "Terra Vegetal 20kg", "24.90", 30, secoes.get("Jardim"));

        produto(jaExistentes, contagem, "SKU-FER-001", "Furadeira de Impacto 650W", "299.90", 7, secoes.get("Ferramentas"));
        produto(jaExistentes, contagem, "SKU-FER-002", "Trena 5m", "24.90", 45, secoes.get("Ferramentas"));

        produto(jaExistentes, contagem, "SKU-DEC-001", "Espelho Redondo 60cm", "159.90", 9, secoes.get("Decoracao"));

        produto(jaExistentes, contagem, "SKU-MAT-001", "Argamassa AC-II 20kg", "28.90", 50, secoes.get("Materiais de construcao"));
        produto(jaExistentes, contagem, "SKU-MAT-002", "Cimento CP-II 50kg", "42.90", 40, secoes.get("Materiais de construcao"));

        /*
         * Pares de substituicao acrescentados para a demonstracao (ver D-47 e O-14).
         *
         * Ate aqui a massa tinha UM unico par plausivel - as duas lixas -, entao zerar
         * qualquer outro produto fazia o assistente recusar, corretamente, e devolver 422.
         * Certo pelo desenho, mas nao e a cena que se quer gravar.
         *
         * Cada item abaixo cumpre a MESMA funcao de um produto ja existente na mesma secao,
         * variando apenas em especificacao - que e como a substituicao acontece numa loja de
         * verdade. Ficam em quatro secoes espalhadas pela loja, para a demonstracao poder
         * partir de qualquer canto do mapa.
         */
        produto(jaExistentes, contagem, "SKU-ILU-003", "Lampada LED 12W Branca - kit 3", "49.90", 35, secoes.get("Iluminacao"));
        produto(jaExistentes, contagem, "SKU-ENC-005", "Sifao Copo Cromado Universal", "39.90", 12, secoes.get("Encanamento"));
        produto(jaExistentes, contagem, "SKU-FER-003", "Trena 7,5m", "34.90", 20, secoes.get("Ferramentas"));
        produto(jaExistentes, contagem, "SKU-MAT-003", "Argamassa AC-III 20kg", "36.90", 28, secoes.get("Materiais de construcao"));
    }

    private void produto(Set<String> jaExistentes, Contagem contagem, String sku, String nome,
                         String preco, int estoque, PontoMapa ponto) {
        if (jaExistentes.contains(sku)) {
            return;
        }
        produtoRepository.salvar(
                new Produto(UUID.randomUUID(), sku, nome, new BigDecimal(preco), estoque, ponto));
        contagem.produtos++;
    }
}
