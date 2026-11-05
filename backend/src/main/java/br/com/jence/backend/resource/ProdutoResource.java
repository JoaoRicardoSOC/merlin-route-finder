package br.com.jence.backend.resource;

import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dto.EstoqueUpdateRequest;
import br.com.jence.backend.dto.StandardError;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.model.AtributoProduto;
import br.com.jence.backend.resource.LeituraDaRequisicao.RequisicaoInvalida;
import br.com.jence.backend.service.BuscarProdutosUseCase;
import br.com.jence.backend.service.ConsultarProdutoUseCase;
import br.com.jence.backend.service.ListarSecoesUseCase;
import br.com.jence.backend.service.SimularEstoqueUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Consulta do catalogo de produtos, mais a ferramenta de simulacao de estoque usada em
 * demonstracao.
 * <p>
 * Segue o padrao do {@code SessaoResource}: apenas recebe, delega e devolve. Os limites de
 * paginacao e o tratamento de termo em branco ficam no caso de uso, que ja e dono dessa regra.
 */
@Path("/api/v1/produtos")
@Tag(name = "Catalogo", description = "Busca e detalhamento de produtos")
public class ProdutoResource {

    private final BuscarProdutosUseCase buscarProdutosUseCase;
    private final ConsultarProdutoUseCase consultarProdutoUseCase;
    private final SimularEstoqueUseCase simularEstoqueUseCase;
    private final ListarSecoesUseCase listarSecoesUseCase;

    @Context
    private UriInfo uriInfo;

    /** Usado pelo Jersey: monta os casos de uso com o DAO de verdade. */
    public ProdutoResource() {
        this(new BuscarProdutosUseCase(new ProdutoDao()),
                new ConsultarProdutoUseCase(new ProdutoDao()),
                new SimularEstoqueUseCase(new ProdutoDao()),
                new ListarSecoesUseCase(new ProdutoDao()));
    }

    /** Usado pelos testes, com casos de uso falsos. */
    public ProdutoResource(BuscarProdutosUseCase buscarProdutosUseCase,
                           ConsultarProdutoUseCase consultarProdutoUseCase,
                           SimularEstoqueUseCase simularEstoqueUseCase,
                           ListarSecoesUseCase listarSecoesUseCase) {
        this.buscarProdutosUseCase = buscarProdutosUseCase;
        this.consultarProdutoUseCase = consultarProdutoUseCase;
        this.simularEstoqueUseCase = simularEstoqueUseCase;
        this.listarSecoesUseCase = listarSecoesUseCase;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Busca e filtragem do catalogo (UC-002)",
            description = "Sem termo, navega o catalogo. Com termo, filtra por nome tolerando "
                    + "busca parcial e pequenos erros de digitacao. Os filtros de secao e de "
                    + "disponibilidade combinam com o termo e entre si, e cada parametro "
                    + "'atributo' vem no formato CHAVE:valor. Valores da mesma chave sao 'ou', "
                    + "chaves diferentes sao 'e'. A resposta traz as facetas disponiveis para o "
                    + "recorte atual. Combinacao sem resultado devolve pagina vazia, nao erro.")
    public Response buscar(
            @QueryParam("query") String query,
            @QueryParam("secao") String secao,
            @QueryParam("apenasDisponiveis") String apenasDisponiveis,
            @QueryParam("atributo") List<String> atributo,
            @QueryParam("page") String page,
            @QueryParam("size") String size) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            // 'query' e o nome publico no contrato; 'termo' e o vocabulario interno.
            return Response.ok(buscarProdutosUseCase.executar(
                    query, secao,
                    LeituraDaRequisicao.logico("apenasDisponiveis", apenasDisponiveis, path),
                    interpretarAtributos(atributo),
                    LeituraDaRequisicao.inteiro("page", page, path),
                    LeituraDaRequisicao.inteiro("size", size, path))).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
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
                // Chave desconhecida e ignorada, nao recusada: vem de um link antigo, e o
                // cliente nao tem o que fazer a respeito (contrato em openapi.yaml).
            }
        }

        return escolhas;
    }

    @GET
    @Path("/secoes")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Listar as secoes do catalogo",
            description = "O menu de navegacao por corredor, com a quantidade de produtos de "
                    + "cada secao. So aparecem secoes que tem produto: uma secao vazia seria "
                    + "um beco sem saida no menu.")
    public Response listarSecoes() {
        String path = uriInfo.getRequestUri().getPath();
        try {
            return Response.ok(listarSecoesUseCase.executar()).build();
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    @GET
    @Path("/{produtoId}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Detalhamento de produto (UC-003)",
            description = "Inclui o ponto de mapa onde o produto esta fisicamente na loja.")
    public Response consultar(@PathParam("produtoId") String produtoId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID produto = LeituraDaRequisicao.uuid("produtoId", produtoId, path);
            return Response.ok(consultarProdutoUseCase.executar(produto)).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    /*
     * Unico endpoint do sistema que altera o catalogo, e o unico que nao serve a nenhum caso
     * de uso do cliente final. Fica aqui, e nao num controller separado, porque o contrato o
     * coloca sob /produtos - e um controller "interno" isolado daria a impressao de haver uma
     * area protegida, que nao existe. O que protege este endpoint hoje e nada; ver D-40.
     */
    @PATCH
    @Path("/{produtoId}/estoque")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "[Demonstracao] Simular alteracao de estoque",
            description = "FERRAMENTA INTERNA - nao faz parte de nenhum caso de uso do cliente "
                    + "final. Zera ou restaura o saldo de um produto sob demanda, para disparar "
                    + "o fluxo de ruptura (UC-013) de forma confiavel durante uma gravacao ou "
                    + "apresentacao ao vivo. Envie 0 para provocar a ruptura e qualquer valor "
                    + "positivo para restaurar.")
    public Response simularEstoque(@PathParam("produtoId") String produtoId, String corpo) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID produto = LeituraDaRequisicao.uuid("produtoId", produtoId, path);
            EstoqueUpdateRequest request =
                    LeituraDaRequisicao.corpo(corpo, EstoqueUpdateRequest.class, path);

            List<StandardError.ValidationError> erros = request.validar();
            if (!erros.isEmpty()) {
                return RespostaDeErro.validacao(path, erros);
            }

            return Response.ok(simularEstoqueUseCase.executar(produto, request.getSaldoEstoque())).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }
}
