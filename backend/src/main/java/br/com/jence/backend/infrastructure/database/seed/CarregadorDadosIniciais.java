package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.BlocoMapa;
import br.com.jence.backend.domain.entity.PlantaDaLoja;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.PontoMapaJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.ProdutoAtributoJpaRepository;
import br.com.jence.backend.infrastructure.database.schema.RestricaoDeEnumNoBanco;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    /*
     * URLs publicas das fotos, coletadas do site da Leroy pelo time (O-18). Enquanto um SKU
     * nao estiver aqui, o produto responde com imagem nula - o que a tela precisa tratar, e
     * nao um estado invalido. Acrescentar uma URL aqui chega aos bancos que ja existem pelo
     * passo de completarApresentacoes.
     */
    private static final Map<String, String> IMAGENS = Map.of();

    /** Descricao e imagem de um produto, para completar o que ja esta gravado. */
    private record Apresentacao(String descricao, String imagemUrl) {
    }

    private final ProdutoRepository produtoRepository;
    private final PontoMapaRepository pontoMapaRepository;
    private final PontoMapaJpaRepository pontoMapaJpaRepository;
    private final ProdutoAtributoJpaRepository atributoJpaRepository;
    private final RestricaoDeEnumNoBanco restricaoDeEnum;

    /*
     * O que esta execucao criou. Vive numa instancia propria por chamada, e nao em campo do
     * componente: como ele e um singleton do Spring, contadores de instancia se somariam entre
     * execucoes e o log passaria a mentir a partir da segunda - foi o que um teste flagrou.
     */
    private static final class Contagem {
        private int pontos;
        private int produtos;
        private int apresentacoes;
        private int atributos;

        private boolean nadaFeito() {
            return pontos == 0 && produtos == 0 && apresentacoes == 0 && atributos == 0;
        }
    }

    /*
     * Montado enquanto o catalogo e declarado, e consumido logo depois para completar o que ja
     * estava gravado. Vive numa instancia por execucao pelo mesmo motivo da Contagem: o
     * componente e um singleton do Spring.
     */
    private final Map<String, Apresentacao> apresentacoes = new LinkedHashMap<>();

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
        /*
          * A ordem importa: apagar as linhas de tipo aposentado antes de refazer a restricao,
          * porque uma linha com valor fora do enum faria o "add check" ser recusado.
          */
        apagarPontosDeTipoAposentado();
        restricaoDeEnum.sincronizar();

        Map<String, PontoMapa> secoes = carregarOuCriarSecoes(contagem);
        criarPontosDeServicoQueFaltam(contagem);
        criarPontosDeQrCodeQueFaltam(contagem);
        apresentacoes.clear();
        criarCatalogo(secoes, skusExistentes, contagem);
        completarApresentacoes(contagem);
        sincronizarAtributos(contagem);

        if (contagem.nadaFeito()) {
            log.info("Massa de demonstracao ja esta completa. Nada a carregar.");
        } else {
            log.info("Carga incremental: {} ponto(s) do mapa, {} produto(s) criados, "
                            + "{} apresentacao(oes) completada(s) e {} produto(s) com "
                            + "caracteristicas atualizadas.",
                    contagem.pontos, contagem.produtos, contagem.apresentacoes, contagem.atributos);
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
        /*
         * A coordenada vem do centro do bloco, e nao de um numero digitado aqui: e o que
         * garante que um produto nunca apareca fora do proprio corredor no mapa. Acrescentar
         * uma secao comeca por acrescentar um bloco em PlantaDaLoja. Ver D-58.
         */
        for (BlocoMapa bloco : PlantaDaLoja.blocos()) {
            registrar(secoes, existentes, contagem, bloco);
        }
        return secoes;
    }

    private void registrar(Map<String, PontoMapa> secoes, Map<String, PontoMapa> existentes,
                           Contagem contagem, BlocoMapa bloco) {
        PontoMapa ponto = existentes.get(bloco.rotulo());
        if (ponto == null) {
            ponto = pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(),
                    TipoPonto.PRATELEIRA, bloco.rotulo(), bloco.centroX(), bloco.centroY()));
            contagem.pontos++;
        }
        secoes.put(bloco.rotulo(), ponto);
    }

    /* Nao vendem nada, mas precisam aparecer no mapa: o cliente vai ate os caixas para
     * fechar a compra, e o banheiro e uma parada que ele pode querer localizar. */
    private void criarPontosDeServicoQueFaltam(Contagem contagem) {
        criarSeNaoHouver(contagem, TipoPonto.CAIXA, "Frente de loja", 62, 88);
        criarSeNaoHouver(contagem, TipoPonto.BANHEIRO, "Sanitarios", 52, 8);
    }

    /*
     * Onde os adesivos ficam colados: corredores de passagem e cruzamentos, nao dentro das
     * secoes - o cliente escaneia enquanto anda, nao quando ja chegou onde queria.
     *
     * O codigo impresso no adesivo leva hifen (ENT-01) porque e mais facil de ler e de ditar;
     * o banco guarda a forma canonica (ENT01) e a busca normaliza a digitacao, entao o hifen e
     * so tipografia. Ver D-52.
     *
     * Quantos e exatamente onde ainda e decisao do time (O-18): trocar as coordenadas aqui nao
     * afeta nenhuma outra parte do sistema.
     */
    private void criarPontosDeQrCodeQueFaltam(Contagem contagem) {
        criarQrCodeSeNaoHouver(contagem, "ENT-01", "Entrada da loja", 50, 92);
        criarQrCodeSeNaoHouver(contagem, "TIN-02", "Corredor de Tintas", 32, 18);
        criarQrCodeSeNaoHouver(contagem, "CEN-03", "Cruzamento central", 41, 40);
        criarQrCodeSeNaoHouver(contagem, "ILU-04", "Corredor leste, junto a Iluminacao", 76, 42);
        criarQrCodeSeNaoHouver(contagem, "FER-05", "Corredor oeste, junto a Ferramentas", 20, 65);
        criarQrCodeSeNaoHouver(contagem, "CAI-06", "Frente de loja, antes dos caixas", 62, 80);
    }

    private void criarQrCodeSeNaoHouver(Contagem contagem, String codigo, String corredor,
                                        int x, int y) {
        if (pontoMapaRepository.buscarPorCodigoCurto(codigo).isEmpty()) {
            pontoMapaRepository.salvar(
                    new PontoMapa(UUID.randomUUID(), TipoPonto.QR_CODE, corredor, x, y, codigo));
            contagem.pontos++;
        }
    }

    private void criarSeNaoHouver(Contagem contagem, TipoPonto tipo, String corredor, int x, int y) {
        if (pontoMapaRepository.buscarPorTipo(tipo).isEmpty()) {
            pontoMapaRepository.salvar(new PontoMapa(UUID.randomUUID(), tipo, corredor, x, y));
            contagem.pontos++;
        }
    }

    // ---------------------------------------------------------------- catalogo

    /**
     * Cria os produtos que ainda nao existem, a partir de {@link CatalogoDaMassa}.
     * <p>
     * Uma fonte so para nome, preco, descricao e caracteristicas: acrescentar um produto e
     * acrescentar uma entrada la, e nao editar tres lugares em paralelo. Ver D-66.
     */
    private void criarCatalogo(Map<String, PontoMapa> secoes, Set<String> jaExistentes,
                               Contagem contagem) {
        for (ProdutoDaMassa declarado : CatalogoDaMassa.produtos()) {
            PontoMapa ponto = secoes.get(declarado.secao());
            if (ponto == null) {
                // Secao sem bloco na planta: o produto ficaria sem lugar no mapa (D-58).
                log.warn("Produto {} declara a secao '{}', que nao existe na planta. Ignorado.",
                        declarado.sku(), declarado.secao());
                continue;
            }

            apresentacoes.put(declarado.sku(),
                    new Apresentacao(declarado.descricao(), IMAGENS.get(declarado.sku())));

            if (jaExistentes.contains(declarado.sku())) {
                continue;
            }

            produtoRepository.salvar(new Produto(UUID.randomUUID(), declarado.sku(),
                    declarado.nome(), declarado.descricao(), IMAGENS.get(declarado.sku()),
                    declarado.precoEmReais(), declarado.estoque(), ponto));
            contagem.produtos++;
        }
    }

    private void sincronizarAtributos(Contagem contagem) {
        Map<String, List<ValorDeAtributo>> declarados = CatalogoDaMassa.produtos().stream()
                .collect(Collectors.toMap(ProdutoDaMassa::sku, ProdutoDaMassa::atributos));

        Map<UUID, List<ValorDeAtributo>> gravados = atributoJpaRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        entity -> entity.getProduto().getId(),
                        Collectors.mapping(
                                entity -> new ValorDeAtributo(entity.getChave(), entity.getValor()),
                                Collectors.toList())));

        for (Produto produto : produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA).conteudo()) {
            List<ValorDeAtributo> esperados = declarados.get(produto.getSku());
            if (esperados == null) {
                continue;
            }

            List<ValorDeAtributo> atuais = gravados.getOrDefault(produto.getId(), List.of());

            if (!Set.copyOf(atuais).equals(Set.copyOf(esperados))) {
                produtoRepository.salvarAtributos(produto.getId(), esperados);
                contagem.atributos++;
            }
        }
    }

    /**
     * Completa descricao e imagem de produtos que ja estavam gravados.
     * <p>
     * A carga e incremental e nunca reescreve um SKU existente (D-47), entao sem este passo os
     * produtos criados antes destes campos existirem ficariam sem apresentacao para sempre -
     * em todos os bancos do time e no publicado.
     * <p>
     * <b>So preenche o que esta vazio.</b> Nunca sobrescreve um texto ja gravado: se alguem
     * ajustar uma descricao direto no banco, a proxima inicializacao nao desfaz.
     */
    private void completarApresentacoes(Contagem contagem) {
        for (Produto produto : produtoRepository.buscarPaginado(0, LIMITE_DE_LEITURA).conteudo()) {
            Apresentacao nova = apresentacoes.get(produto.getSku());
            if (nova == null) {
                continue;
            }

            String descricao = produto.getDescricao() == null ? nova.descricao() : produto.getDescricao();
            String imagem = produto.getImagemUrl() == null ? nova.imagemUrl() : produto.getImagemUrl();

            boolean mudou = !java.util.Objects.equals(descricao, produto.getDescricao())
                    || !java.util.Objects.equals(imagem, produto.getImagemUrl());

            if (mudou) {
                produtoRepository.salvar(produto.comApresentacao(descricao, imagem));
                contagem.apresentacoes++;
            }
        }
    }
}
