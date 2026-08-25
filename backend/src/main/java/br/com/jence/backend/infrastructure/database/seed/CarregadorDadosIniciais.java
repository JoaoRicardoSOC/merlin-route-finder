package br.com.jence.backend.infrastructure.database.seed;

import br.com.jence.backend.domain.entity.BlocoMapa;
import br.com.jence.backend.domain.entity.PlantaDaLoja;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.PontoMapaJpaRepository;
import br.com.jence.backend.infrastructure.database.schema.RestricaoDeEnumNoBanco;
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

        private boolean nadaFeito() {
            return pontos == 0 && produtos == 0 && apresentacoes == 0;
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

        if (contagem.nadaFeito()) {
            log.info("Massa de demonstracao ja esta completa. Nada a carregar.");
        } else {
            log.info("Carga incremental: {} ponto(s) do mapa, {} produto(s) criados e "
                            + "{} apresentacao(oes) completada(s).",
                    contagem.pontos, contagem.produtos, contagem.apresentacoes);
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

    private void criarCatalogo(Map<String, PontoMapa> secoes, Set<String> jaExistentes,
                               Contagem contagem) {
        /*
         * Tintas concentra o cenario de ruptura de estoque (UC-013): a lixa grao 120 esta
         * zerada e a lixa d'agua grao 150, na mesma secao, e o substituto plausivel que a IA
         * deve encontrar na pre-filtragem espacial.
         */
        produto(jaExistentes, contagem, "SKU-TIN-001", "Tinta Acrilica Fosca Branca 18L", "289.90", 12, secoes.get("Tintas"),
                "Tinta acrilica de acabamento fosco para paredes internas e externas. O balde de 18 litros rende cerca de 350 m2 por demao e disfarca imperfeicoes da superficie melhor que os acabamentos brilhantes.");
        produto(jaExistentes, contagem, "SKU-TIN-002", "Rolo de La 23cm com Cabo", "34.90", 25, secoes.get("Tintas"),
                "Rolo de la de carneiro 23 cm com cabo, para aplicar tinta acrilica ou latex em grandes areas. A la solta pouco pelo e nao deixa marca de emenda em parede lisa.");
        produto(jaExistentes, contagem, "SKU-TIN-003", "Lixa para Parede Grao 120", "3.50", 0, secoes.get("Tintas"),
                "Lixa de papel grao 120 para preparar parede antes da pintura. Grao medio: remove respingos e nivela massa corrida sem abrir sulcos no reboco.");
        produto(jaExistentes, contagem, "SKU-TIN-004", "Lixa d'Agua Grao 150", "4.20", 40, secoes.get("Tintas"),
                "Lixa d'agua grao 150 para acabamento fino em parede, madeira e metal. Usada umida, produz menos po e entope menos que a lixa comum.");
        produto(jaExistentes, contagem, "SKU-TIN-005", "Fita Crepe 48mm x 50m", "12.90", 30, secoes.get("Tintas"),
                "Fita crepe 48 mm x 50 m para proteger rodape, batente e tomada durante a pintura. Sai sem deixar residuo se removida em ate 24 horas.");

        produto(jaExistentes, contagem, "SKU-FRG-001", "Parafuso Chipboard 4x40mm - 100un", "19.90", 80, secoes.get("Ferragens"),
                "Caixa com 100 parafusos chipboard 4x40 mm, cabeca chata e rosca soberba. Indicados para MDF, aglomerado e madeira macica sem necessidade de pre-furo em muitos casos.");
        produto(jaExistentes, contagem, "SKU-FRG-002", "Bucha de Nylon 8mm - 50un", "15.90", 65, secoes.get("Ferragens"),
                "Pacote com 50 buchas de nylon 8 mm para fixacao em alvenaria, concreto e bloco. Acompanham o furo de broca 8 mm e suportam prateleiras e suportes de TV leves.");

        produto(jaExistentes, contagem, "SKU-ELE-001", "Cabo Flexivel 2,5mm 100m", "189.90", 8, secoes.get("Eletrica"),
                "Rolo de 100 m de cabo flexivel 2,5 mm2, isolacao 750 V. Bitola usada em circuitos de tomadas de uso geral em residencias.");
        produto(jaExistentes, contagem, "SKU-ELE-002", "Interruptor Simples Branco", "14.90", 50, secoes.get("Eletrica"),
                "Interruptor simples de embutir, uma tecla, acabamento branco. Liga e desliga um ponto de luz a partir de um unico local.");
        produto(jaExistentes, contagem, "SKU-ELE-003", "Disjuntor Bipolar 25A", "42.90", 15, secoes.get("Eletrica"),
                "Disjuntor termomagnetico bipolar 25 A, padrao DIN. Protege circuitos de chuveiro e ar-condicionado contra sobrecarga e curto-circuito.");

        produto(jaExistentes, contagem, "SKU-ENC-001", "Cano PVC Soldavel 25mm 6m", "28.90", 35, secoes.get("Encanamento"),
                "Barra de 6 m de cano PVC soldavel 25 mm para agua fria. Bitola mais comum em ramais de banheiro e cozinha em residencias.");
        produto(jaExistentes, contagem, "SKU-ENC-002", "Cola para PVC 175g", "18.90", 22, secoes.get("Encanamento"),
                "Adesivo plastico 175 g para soldar conexoes de PVC rigido. A junta pode receber agua depois de 12 horas de cura.");
        produto(jaExistentes, contagem, "SKU-ENC-003", "Torneira Cromada para Banheiro", "129.90", 10, secoes.get("Encanamento"),
                "Torneira de mesa para lavatorio de banheiro, acabamento cromado, bica baixa. Rosca padrao de 1/2 polegada.");
        produto(jaExistentes, contagem, "SKU-ENC-004", "Sifao Sanfonado Universal", "22.50", 18, secoes.get("Encanamento"),
                "Sifao sanfonado universal para pia e lavatorio. O corpo flexivel se ajusta a distancias diferentes entre o ralo e a parede, o que resolve instalacoes fora do esquadro.");

        produto(jaExistentes, contagem, "SKU-COZ-001", "Cuba Inox 56x33cm", "249.90", 6, secoes.get("Cozinhas"),
                "Cuba de aco inox 56x33 cm para bancada de cozinha, com valvula. Profundidade que acomoda panela grande sem respingar.");
        produto(jaExistentes, contagem, "SKU-COZ-002", "Torneira Gourmet Cromada", "389.90", 4, secoes.get("Cozinhas"),
                "Torneira gourmet de mesa com bica alta movel e acabamento cromado. A altura livre facilita encher panelas e jarras.");

        produto(jaExistentes, contagem, "SKU-ILU-001", "Lampada LED 9W Branca - kit 3", "39.90", 60, secoes.get("Iluminacao"),
                "Kit com 3 lampadas LED 9 W, luz branca, soquete E27. Cerca de 900 lumens cada, equivalentes a lampadas incandescentes de 60 W.");
        produto(jaExistentes, contagem, "SKU-ILU-002", "Luminaria de Embutir Quadrada", "69.90", 14, secoes.get("Iluminacao"),
                "Luminaria quadrada de embutir para forro de gesso, com recorte de 17 cm. Acompanha soquete e mola de fixacao.");

        produto(jaExistentes, contagem, "SKU-JAR-001", "Vaso de Ceramica 30cm", "79.90", 12, secoes.get("Jardim"),
                "Vaso de ceramica esmaltada 30 cm de diametro, com furo de drenagem. Indicado para plantas de porte medio em area interna ou varanda.");
        produto(jaExistentes, contagem, "SKU-JAR-002", "Terra Vegetal 20kg", "24.90", 30, secoes.get("Jardim"),
                "Saco de 20 kg de terra vegetal adubada, pronta para uso em vasos, canteiros e replantio. Nao precisa de correcao antes do plantio.");

        produto(jaExistentes, contagem, "SKU-FER-001", "Furadeira de Impacto 650W", "299.90", 7, secoes.get("Ferramentas"),
                "Furadeira de impacto 650 W com mandril de 1/2 polegada e reversao. O modo impacto perfura concreto e alvenaria; sem impacto, madeira e metal.");
        produto(jaExistentes, contagem, "SKU-FER-002", "Trena 5m", "24.90", 45, secoes.get("Ferramentas"),
                "Trena de 5 m com fita de aco, trava e clipe de cinto. Fita de 19 mm, que mantem a rigidez em medidas longas sem apoio.");

        produto(jaExistentes, contagem, "SKU-DEC-001", "Espelho Redondo 60cm", "159.90", 9, secoes.get("Decoracao"),
                "Espelho redondo de 60 cm com moldura fina e sistema de fixacao incluso. Amplia visualmente ambientes pequenos como lavabo e corredor.");

        produto(jaExistentes, contagem, "SKU-MAT-001", "Argamassa AC-II 20kg", "28.90", 50, secoes.get("Materiais de construcao"),
                "Argamassa colante AC-II, saco de 20 kg, para assentamento de ceramica em area interna e externa. Suporta variacao de temperatura e umidade.");
        produto(jaExistentes, contagem, "SKU-MAT-002", "Cimento CP-II 50kg", "42.90", 40, secoes.get("Materiais de construcao"),
                "Saco de 50 kg de cimento Portland CP-II, de uso geral em concreto, argamassa e assentamento de blocos.");

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
        produto(jaExistentes, contagem, "SKU-ILU-003", "Lampada LED 12W Branca - kit 3", "49.90", 35, secoes.get("Iluminacao"),
                "Kit com 3 lampadas LED 12 W, luz branca, soquete E27. Cerca de 1.250 lumens cada, indicadas para sala e cozinha.");
        produto(jaExistentes, contagem, "SKU-ENC-005", "Sifao Copo Cromado Universal", "39.90", 12, secoes.get("Encanamento"),
                "Sifao copo cromado universal para pia e lavatorio. O copo retem residuos e pode ser aberto para limpeza sem desmontar a instalacao.");
        produto(jaExistentes, contagem, "SKU-FER-003", "Trena 7,5m", "34.90", 20, secoes.get("Ferramentas"),
                "Trena de 7,5 m com fita de aco, trava e clipe de cinto. O alcance extra cobre comodos inteiros e vaos de parede numa medida so.");
        produto(jaExistentes, contagem, "SKU-MAT-003", "Argamassa AC-III 20kg", "36.90", 28, secoes.get("Materiais de construcao"),
                "Argamassa colante AC-III, saco de 20 kg, de aderencia reforcada. Indicada para porcelanato, pecas grandes e assentamento sobre piso antigo.");
    }

    private void produto(Set<String> jaExistentes, Contagem contagem, String sku, String nome,
                         String preco, int estoque, PontoMapa ponto, String descricao) {
        apresentacoes.put(sku, new Apresentacao(descricao, IMAGENS.get(sku)));

        if (jaExistentes.contains(sku)) {
            return;
        }
        produtoRepository.salvar(new Produto(UUID.randomUUID(), sku, nome, descricao,
                IMAGENS.get(sku), new BigDecimal(preco), estoque, ponto));
        contagem.produtos++;
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
