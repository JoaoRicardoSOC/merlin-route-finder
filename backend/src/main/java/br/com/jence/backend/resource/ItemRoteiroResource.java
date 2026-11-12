package br.com.jence.backend.resource;

import br.com.jence.backend.dao.ItemRoteiroDao;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.RegistroRupturaDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.dto.StandardError;
import br.com.jence.backend.dto.SubstituirItemRequest;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.ia.GeminiClient;
import br.com.jence.backend.resource.LeituraDaRequisicao.RequisicaoInvalida;
import br.com.jence.backend.service.DesmarcarItemColetadoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.service.SubstituirItemDoRoteiroUseCase;
import br.com.jence.backend.service.TratarRupturaEstoqueUseCase;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.util.List;
import java.util.UUID;

/**
 * Acoes sobre um item durante a caminhada pela loja.
 * <p>
 * Resource separado do de roteiro porque estas rotas nao levam a sessao no caminho: o
 * celular ja tem o id do item vindo da lista de compras.
 */
@Path("/api/v1/roteiro/itens")
public class ItemRoteiroResource {

    /*
     * Uma instancia so para todas as requisicoes: o Jersey cria um Resource novo a cada
     * chamada, e sem o static cada ruptura abriria um HttpClient novo para o Gemini.
     */
    private static final GeminiClient GEMINI = new GeminiClient();

    private final MarcarItemColetadoUseCase marcarItemColetadoUseCase;
    private final DesmarcarItemColetadoUseCase desmarcarItemColetadoUseCase;
    private final TratarRupturaEstoqueUseCase tratarRupturaEstoqueUseCase;
    private final SubstituirItemDoRoteiroUseCase substituirItemDoRoteiroUseCase;

    @Context
    private UriInfo uriInfo;

    /** Usado pelo Jersey: monta os casos de uso com os DAOs e o cliente do Gemini de verdade. */
    public ItemRoteiroResource() {
        this(new MarcarItemColetadoUseCase(new ListaRoteiroDao(), new SessaoDao()),
                new DesmarcarItemColetadoUseCase(new ListaRoteiroDao(), new SessaoDao()),
                new TratarRupturaEstoqueUseCase(new ItemRoteiroDao(), new ListaRoteiroDao(),
                        new SessaoDao(), new ProdutoDao(), new RegistroRupturaDao(), GEMINI),
                new SubstituirItemDoRoteiroUseCase(new ListaRoteiroDao(), new SessaoDao(), new ProdutoDao()));
    }

    /** Usado pelos testes, com casos de uso falsos. */
    public ItemRoteiroResource(MarcarItemColetadoUseCase marcarItemColetadoUseCase,
                               DesmarcarItemColetadoUseCase desmarcarItemColetadoUseCase,
                               TratarRupturaEstoqueUseCase tratarRupturaEstoqueUseCase,
                               SubstituirItemDoRoteiroUseCase substituirItemDoRoteiroUseCase) {
        this.marcarItemColetadoUseCase = marcarItemColetadoUseCase;
        this.desmarcarItemColetadoUseCase = desmarcarItemColetadoUseCase;
        this.tratarRupturaEstoqueUseCase = tratarRupturaEstoqueUseCase;
        this.substituirItemDoRoteiroUseCase = substituirItemDoRoteiroUseCase;
    }

    @PATCH
    @Path("/{itemId}/coletar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response coletar(@PathParam("itemId") String itemId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID item = LeituraDaRequisicao.uuid("itemId", itemId, path);
            return Response.ok(marcarItemColetadoUseCase.executar(item)).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (OperacaoNaoPermitidaException e) {
            return RespostaDeErro.operacaoNaoPermitida(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    @PATCH
    @Path("/{itemId}/desmarcar")
    @Produces(MediaType.APPLICATION_JSON)
    public Response desmarcar(@PathParam("itemId") String itemId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID item = LeituraDaRequisicao.uuid("itemId", itemId, path);
            return Response.ok(desmarcarItemColetadoUseCase.executar(item)).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (OperacaoNaoPermitidaException e) {
            return RespostaDeErro.operacaoNaoPermitida(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    @POST
    @Path("/{itemId}/substituir")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response substituir(@PathParam("itemId") String itemId, String corpo) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID item = LeituraDaRequisicao.uuid("itemId", itemId, path);
            SubstituirItemRequest requisicao =
                    LeituraDaRequisicao.corpo(corpo, SubstituirItemRequest.class, path);

            List<StandardError.ValidationError> erros = requisicao.validar();
            if (!erros.isEmpty()) {
                return RespostaDeErro.validacao(path, erros);
            }

            return Response.ok(substituirItemDoRoteiroUseCase.executar(
                    item, requisicao.getProdutoSubstitutoId())).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (OperacaoNaoPermitidaException e) {
            return RespostaDeErro.operacaoNaoPermitida(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    /*
     * POST e nao PATCH: nao e a atualizacao de um campo do item, e o relato de um fato novo -
     * a gondola estava vazia - que fica registrado no banco. Tambem nao e idempotente: dois
     * toques no botao sao duas visitas frustradas a prateleira, e a loja precisa das duas.
     */
    @POST
    @Path("/{itemId}/ruptura")
    @Produces(MediaType.APPLICATION_JSON)
    public Response relatarRuptura(@PathParam("itemId") String itemId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID item = LeituraDaRequisicao.uuid("itemId", itemId, path);
            return Response.ok(tratarRupturaEstoqueUseCase.executar(item)).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (OperacaoNaoPermitidaException e) {
            return RespostaDeErro.operacaoNaoPermitida(e, path);
        } catch (SubstitutoIndisponivelException e) {
            return RespostaDeErro.substitutoIndisponivel(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }
}
