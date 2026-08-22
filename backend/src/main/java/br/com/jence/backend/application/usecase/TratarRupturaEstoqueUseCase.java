package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.RupturaEstoqueResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.OrigemSugestao;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;
import br.com.jence.backend.domain.entity.RegistroRuptura;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.AssistenteIAIndisponivelException;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.domain.repository.ItemRoteiroRepository;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.RegistroRupturaRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.AssistenteIA;
import br.com.jence.backend.domain.service.MensagemIA;
import br.com.jence.backend.infrastructure.ia.factory.InstrucaoDeRuptura;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UC-013: o cliente chega a prateleira e ela esta vazia.
 * <p>
 * E a peca que assume que a divergencia entre o estoque do sistema e a gondola <b>vai</b>
 * acontecer (ver D-23) e converte a falha em oportunidade de venda: registra a ruptura,
 * filtra no banco o que a loja tem disponivel fisicamente perto dali e deixa o assistente
 * eleger, entre esses candidatos e somente eles, o que cumpre a mesma funcao.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TratarRupturaEstoqueUseCase {

    /*
     * Raio no grid da planta da loja (0-100, ver CarregadorDadosIniciais). Em 25 unidades, um
     * produto de Tintas alcanca Ferragens e Eletrica, mas nao o outro extremo da loja - a
     * ordem de grandeza de um desvio que o cliente aceita fazer a pe, que e o criterio que
     * importa aqui.
     */
    public static final double RAIO_DE_BUSCA = 25.0;

    /** Teto de candidatos enviados ao assistente, para o julgamento nao virar um catalogo. */
    public static final int LIMITE_DE_CANDIDATOS = 10;

    private final ItemRoteiroRepository itemRoteiroRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;
    private final SessaoRepository sessaoRepository;
    private final ProdutoRepository produtoRepository;
    private final RegistroRupturaRepository registroRupturaRepository;
    private final AssistenteIA assistenteIA;

    @Transactional
    public RupturaEstoqueResponse executar(UUID itemId) {
        ItemRoteiro item = itemRoteiroRepository.buscarPorId(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item de roteiro", itemId));

        ListaRoteiro lista = listaRoteiroRepository.buscarPorItem(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista do item de roteiro", itemId));

        Sessao sessao = sessaoRepository.buscarPorId(lista.getSessaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", lista.getSessaoId()));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessao.getId(), sessao.getStatus()));
        }

        // O cliente esta caminhando pela loja: cada acao dele renova o TTL (D-24).
        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        Produto emFalta = item.getProduto();
        List<Produto> candidatos = candidatosProximosDe(emFalta, lista);

        if (candidatos.isEmpty()) {
            return recusar(sessao.getId(), item,
                    "Nenhum produto com estoque esta num raio caminhavel deste ponto.");
        }

        Sugestao sugestao = eleger(emFalta, candidatos);

        if (!sugestao.temProduto()) {
            return recusar(sessao.getId(), item, sugestao.justificativa());
        }

        registroRupturaRepository.salvar(RegistroRuptura.comSugestao(
                sessao.getId(), item.getId(), emFalta.getId(),
                sugestao.produto().getId(), sugestao.justificativa(), sugestao.origem()));

        log.info("Ruptura na sessao {}: '{}' em falta, sugerido '{}' (origem {})",
                sessao.getId(), emFalta.getNome(), sugestao.produto().getNome(), sugestao.origem());

        return RupturaEstoqueResponse.de(
                emFalta.getId(), sugestao.produto(), sugestao.justificativa(), sugestao.origem());
    }

    /*
     * A ruptura e registrada mesmo quando nao ha o que oferecer, e so depois a requisicao
     * falha com 422. Para a loja, "o cliente foi ate a prateleira e nao havia nada, nem
     * substituto" e justamente o relato mais grave a chegar.
     */
    private RupturaEstoqueResponse recusar(UUID sessaoId, ItemRoteiro item, String motivo) {
        registroRupturaRepository.salvar(RegistroRuptura.semSugestao(
                sessaoId, item.getId(), item.getProduto().getId(), motivo));

        throw new SubstitutoIndisponivelException(
                "Nenhum substituto plausivel para '%s' foi encontrado nas proximidades. %s"
                        .formatted(item.getProduto().getNome(), motivo));
    }

    // ---------------------------------------------------------------- pre-filtragem espacial

    private List<Produto> candidatosProximosDe(Produto emFalta, ListaRoteiro lista) {
        PontoMapa origem = emFalta.getPontoMapa();
        if (origem == null) {
            // Sem posicao conhecida nao existe "perto": nao ha como filtrar espacialmente.
            log.warn("Produto {} nao tem ponto no mapa; ruptura sem candidatos", emFalta.getSku());
            return List.of();
        }

        Set<UUID> jaNoRoteiro = lista.getItens().stream()
                .map(i -> i.getProduto().getId())
                .collect(Collectors.toSet());

        return produtoRepository
                .buscarDisponiveisProximosDe(origem, emFalta.getId(), RAIO_DE_BUSCA, LIMITE_DE_CANDIDATOS)
                .stream()
                // Sugerir o que o cliente ja vai levar nao ajuda ninguem.
                .filter(p -> !jaNoRoteiro.contains(p.getId()))
                .toList();
    }

    // ---------------------------------------------------------------- eleicao do substituto

    private Sugestao eleger(Produto emFalta, List<Produto> candidatos) {
        try {
            String resposta = assistenteIA.conversar(
                    InstrucaoDeRuptura.instrucaoDeSistema(emFalta.getNome(), corredorDe(emFalta)),
                    List.of(MensagemIA.doCliente(
                            "Estou na prateleira e o produto acabou. O que voce indica no lugar?")),
                    InstrucaoDeRuptura.ferramentas(),
                    (ferramenta, argumentos) -> Map.of("candidatos", descrever(candidatos, emFalta)));

            return interpretar(resposta, candidatos);

        } catch (AssistenteIAIndisponivelException e) {
            /*
             * Fallback previsto na D-35: o cliente esta parado diante da prateleira esperando
             * uma resposta, e a cota do tier gratuito estoura com facilidade. Cair para o
             * disponivel mais proximo entrega algo util sem fingir que houve analise.
             */
            log.warn("Assistente indisponivel na ruptura de '{}': {}", emFalta.getSku(), e.getMessage());
            return Sugestao.porProximidade(candidatos.getFirst());
        }
    }

    /*
     * O grounding mora aqui. O modelo devolve texto, e texto e a unica coisa que aceitamos
     * dele: o SKU so vale se estiver entre os candidatos que NOS oferecemos, e o produto
     * entregue ao cliente e sempre o objeto vindo do nosso banco. Um codigo inventado, ou de
     * um produto que existe mas nao estava na lista, e descartado como se a IA nao tivesse
     * respondido. Ver D-38.
     */
    private Sugestao interpretar(String resposta, List<Produto> candidatos) {
        String[] partes = resposta.strip().split("\\" + InstrucaoDeRuptura.SEPARADOR, 2);
        String codigo = normalizar(partes[0]);
        String justificativa = partes.length > 1 ? partes[1].strip().replaceAll("\\s+", " ") : "";

        if (InstrucaoDeRuptura.NENHUM.equalsIgnoreCase(codigo)) {
            return Sugestao.recusada(justificativa.isBlank()
                    ? "O assistente nao encontrou nenhum substituto adequado por perto."
                    : justificativa);
        }

        Optional<Produto> escolhido = candidatos.stream()
                .filter(p -> p.getSku().equalsIgnoreCase(codigo))
                .findFirst();

        if (escolhido.isEmpty() || justificativa.isBlank()) {
            log.warn("Assistente respondeu fora do combinado ('{}'); usando o disponivel mais proximo",
                    resposta.strip());
            return Sugestao.porProximidade(candidatos.getFirst());
        }

        return new Sugestao(escolhido.get(), justificativa, OrigemSugestao.ASSISTENTE_IA);
    }

    /* Modelos gostam de negrito e crase; o codigo em si so tem letras, numeros e hifen. */
    private String normalizar(String codigo) {
        return codigo.replaceAll("[^A-Za-z0-9-]", "").strip();
    }

    private List<Map<String, Object>> descrever(List<Produto> candidatos, Produto emFalta) {
        List<Map<String, Object>> descritos = new ArrayList<>(candidatos.size());
        for (Produto p : candidatos) {
            descritos.add(Map.of(
                    "sku", p.getSku(),
                    "nome", p.getNome(),
                    "preco", p.getPreco().toString(),
                    "corredor", corredorDe(p),
                    "distancia", Math.round(distanciaEntre(emFalta, p))));
        }
        return descritos;
    }

    private double distanciaEntre(Produto origem, Produto destino) {
        if (origem.getPontoMapa() == null || destino.getPontoMapa() == null) {
            return 0;
        }
        return origem.getPontoMapa().calcularDistanciaPara(destino.getPontoMapa());
    }

    private String corredorDe(Produto produto) {
        return produto.getPontoMapa() != null ? produto.getPontoMapa().getCorredor() : "nao informado";
    }

    /**
     * O substituto eleito, com a marca de quem elegeu. {@code produto} nulo significa que o
     * assistente avaliou os candidatos e concluiu que nenhum serve - situacao diferente de
     * "nao havia candidatos", e que por isso nao cai no fallback de proximidade.
     */
    private record Sugestao(Produto produto, String justificativa, OrigemSugestao origem) {

        static Sugestao porProximidade(Produto maisProximo) {
            return new Sugestao(maisProximo,
                    "Este e o produto disponivel mais proximo de onde voce esta. Confira na "
                            + "embalagem se ele atende ao seu caso antes de levar.",
                    OrigemSugestao.PROXIMIDADE);
        }

        static Sugestao recusada(String motivo) {
            return new Sugestao(null, motivo, OrigemSugestao.NENHUMA);
        }

        boolean temProduto() {
            return produto != null;
        }
    }
}
