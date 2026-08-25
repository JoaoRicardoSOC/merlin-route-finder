package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.CatalogoResponse;
import br.com.jence.backend.application.dto.EstoqueUpdateRequest;
import br.com.jence.backend.application.dto.PaginaResponse;
import br.com.jence.backend.application.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.dto.SecaoResponse;
import br.com.jence.backend.application.usecase.BuscarProdutosUseCase;
import br.com.jence.backend.application.usecase.ConsultarProdutoUseCase;
import br.com.jence.backend.application.usecase.ListarSecoesUseCase;
import br.com.jence.backend.application.usecase.SimularEstoqueUseCase;
import br.com.jence.backend.domain.entity.AtributoProduto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consulta do catalogo de produtos, mais a ferramenta de simulacao de estoque usada em
 * demonstracao.
 * <p>
 * Segue o padrao do {@code SessaoController}: apenas recebe, delega e devolve. Os limites de
 * paginacao e o tratamento de termo em branco ficam no caso de uso, que ja e dono dessa regra.
 */
@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Catalogo", description = "Busca e detalhamento de produtos")
public class ProdutoController {

    private final BuscarProdutosUseCase buscarProdutosUseCase;
    private final ConsultarProdutoUseCase consultarProdutoUseCase;
    private final SimularEstoqueUseCase simularEstoqueUseCase;
    private final ListarSecoesUseCase listarSecoesUseCase;

    @GetMapping
    @Operation(summary = "Busca e filtragem do catalogo (UC-002)",
            description = "Sem termo, navega o catalogo. Com termo, filtra por nome tolerando "
                    + "busca parcial e pequenos erros de digitacao. Os filtros de secao e de "
                    + "disponibilidade combinam com o termo e entre si, e cada parametro "
                    + "'atributo' vem no formato CHAVE:valor. Valores da mesma chave sao 'ou', "
                    + "chaves diferentes sao 'e'. A resposta traz as facetas disponiveis para o "
                    + "recorte atual. Combinacao sem resultado devolve pagina vazia, nao erro.")
    public ResponseEntity<CatalogoResponse> buscar(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String secao,
            @RequestParam(required = false) Boolean apenasDisponiveis,
            @RequestParam(required = false) List<String> atributo,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        // 'query' e o nome publico no contrato; 'termo' e o vocabulario interno.
        return ResponseEntity.ok(buscarProdutosUseCase.executar(
                query, secao, apenasDisponiveis, interpretarAtributos(atributo), page, size));
    }

    /*
     * Cada 'atributo' chega como "CHAVE:valor", e a chave pode se repetir - e assim que o
     * cliente marca duas marcas de uma vez.
     *
     * Entrada que nao da para interpretar e IGNORADA, nao recusada: chave desconhecida vem de
     * um link antigo ou de uma caracteristica que saiu do sistema, e o cliente nao tem o que
     * fazer a respeito. Ignorar apenas alarga o resultado; um 400 deixaria a tela em branco
     * por causa de um parametro que ele nem sabe que existe. Mesmo criterio da D-57.
     */
    private Map<AtributoProduto, List<String>> interpretarAtributos(List<String> parametros) {
        if (parametros == null || parametros.isEmpty()) {
            return Map.of();
        }

        Map<AtributoProduto, List<String>> escolhas = new LinkedHashMap<>();

        for (String parametro : parametros) {
            int separador = parametro == null ? -1 : parametro.indexOf(':');
            if (separador <= 0) {
                continue;
            }
            try {
                AtributoProduto chave = AtributoProduto.valueOf(
                        parametro.substring(0, separador).trim().toUpperCase());

                escolhas.computeIfAbsent(chave, k -> new ArrayList<>())
                        .add(parametro.substring(separador + 1));
            } catch (IllegalArgumentException chaveDesconhecida) {
                log.debug("Filtro de atributo ignorado, chave desconhecida: {}", parametro);
            }
        }

        return escolhas;
    }

    @GetMapping("/secoes")
    @Operation(summary = "Listar as secoes do catalogo",
            description = "O menu de navegacao por corredor, com a quantidade de produtos de "
                    + "cada secao. So aparecem secoes que tem produto: uma secao vazia seria "
                    + "um beco sem saida no menu.")
    public ResponseEntity<List<SecaoResponse>> listarSecoes() {
        return ResponseEntity.ok(listarSecoesUseCase.executar());
    }

    @GetMapping("/{produtoId}")
    @Operation(summary = "Detalhamento de produto (UC-003)",
            description = "Inclui o ponto de mapa onde o produto esta fisicamente na loja.")
    public ResponseEntity<ProdutoDetalhadoResponse> consultar(@PathVariable UUID produtoId) {
        return ResponseEntity.ok(consultarProdutoUseCase.executar(produtoId));
    }

    /*
     * Unico endpoint do sistema que altera o catalogo, e o unico que nao serve a nenhum caso
     * de uso do cliente final. Fica aqui, e nao num controller separado, porque o contrato o
     * coloca sob /produtos - e um controller "interno" isolado daria a impressao de haver uma
     * area protegida, que nao existe. O que protege este endpoint hoje e nada; ver D-40.
     */
    @PatchMapping("/{produtoId}/estoque")
    @Operation(summary = "[Demonstracao] Simular alteracao de estoque",
            description = "FERRAMENTA INTERNA - nao faz parte de nenhum caso de uso do cliente "
                    + "final. Zera ou restaura o saldo de um produto sob demanda, para disparar "
                    + "o fluxo de ruptura (UC-013) de forma confiavel durante uma gravacao ou "
                    + "apresentacao ao vivo. Envie 0 para provocar a ruptura e qualquer valor "
                    + "positivo para restaurar.")
    public ResponseEntity<ProdutoResponse> simularEstoque(
            @PathVariable UUID produtoId,
            @Valid @RequestBody EstoqueUpdateRequest request) {

        return ResponseEntity.ok(simularEstoqueUseCase.executar(produtoId, request.saldoEstoque()));
    }
}
