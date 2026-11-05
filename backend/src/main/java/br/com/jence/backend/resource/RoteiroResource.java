package br.com.jence.backend.resource;

import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.dto.AdicionarItemRequest;
import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.dto.StandardError;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.resource.LeituraDaRequisicao.RequisicaoInvalida;
import br.com.jence.backend.service.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.service.ConsultarListaRoteiroUseCase;
import br.com.jence.backend.service.RemoverProdutoDoRoteiroUseCase;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
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
 * Montagem do carrinho de roteiro no Totem.
 * <p>
 * Resource separado do de sessao apesar de compartilhar o prefixo da URL: o recurso e outro
 * e tem casos de uso proprios.
 */
@Path("/api/v1/sessoes/{sessaoId}/roteiro")
public class RoteiroResource {

    private final ConsultarListaRoteiroUseCase consultarListaRoteiroUseCase;
    private final AdicionarProdutoAoRoteiroUseCase adicionarProdutoAoRoteiroUseCase;
    private final RemoverProdutoDoRoteiroUseCase removerProdutoDoRoteiroUseCase;

    @Context
    private UriInfo uriInfo;

    /** Usado pelo Jersey: monta os casos de uso com os DAOs de verdade. */
    public RoteiroResource() {
        this(new ConsultarListaRoteiroUseCase(new ListaRoteiroDao()),
                new AdicionarProdutoAoRoteiroUseCase(new SessaoDao(), new ListaRoteiroDao(), new ProdutoDao()),
                new RemoverProdutoDoRoteiroUseCase(new SessaoDao(), new ListaRoteiroDao()));
    }

    /** Usado pelos testes, com casos de uso falsos. */
    public RoteiroResource(ConsultarListaRoteiroUseCase consultarListaRoteiroUseCase,
                           AdicionarProdutoAoRoteiroUseCase adicionarProdutoAoRoteiroUseCase,
                           RemoverProdutoDoRoteiroUseCase removerProdutoDoRoteiroUseCase) {
        this.consultarListaRoteiroUseCase = consultarListaRoteiroUseCase;
        this.adicionarProdutoAoRoteiroUseCase = adicionarProdutoAoRoteiroUseCase;
        this.removerProdutoDoRoteiroUseCase = removerProdutoDoRoteiroUseCase;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultar(@PathParam("sessaoId") String sessaoId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            return Response.ok(consultarListaRoteiroUseCase.executar(sessao)).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    /*
     * Sem header Location de proposito: ele deveria apontar para uma URL que responde GET, e
     * /roteiro/itens/{itemId} so aceita DELETE. O item criado vai no corpo, que e o que o
     * Totem precisa para atualizar a tela.
     */
    @POST
    @Path("/itens")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response adicionar(@PathParam("sessaoId") String sessaoId, String corpo) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            AdicionarItemRequest request =
                    LeituraDaRequisicao.corpo(corpo, AdicionarItemRequest.class, path);

            List<StandardError.ValidationError> erros = request.validar();
            if (!erros.isEmpty()) {
                return RespostaDeErro.validacao(path, erros);
            }

            ItemRoteiroDetalhadoResponse item =
                    adicionarProdutoAoRoteiroUseCase.executar(sessao, request.getProdutoId());

            return Response.status(Response.Status.CREATED).entity(item).build();
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

    @DELETE
    @Path("/itens/{itemId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response remover(@PathParam("sessaoId") String sessaoId,
                            @PathParam("itemId") String itemId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            removerProdutoDoRoteiroUseCase.executar(
                    LeituraDaRequisicao.uuid("sessaoId", sessaoId, path),
                    LeituraDaRequisicao.uuid("itemId", itemId, path));
            return Response.noContent().build();
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
}
