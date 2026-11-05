package br.com.jence.backend.resource;

import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.dto.IniciarSessaoRequest;
import br.com.jence.backend.dto.RecentrarSessaoRequest;
import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.dto.StandardError;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.resource.LeituraDaRequisicao.RequisicaoInvalida;
import br.com.jence.backend.service.ConcluirRotaUseCase;
import br.com.jence.backend.service.ConsultarSessaoUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.RecentrarSessaoUseCase;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Path("/api/v1/sessoes")
public class SessaoResource {

    private final InicializarSessaoUseCase inicializarSessaoUseCase;
    private final ConsultarSessaoUseCase consultarSessaoUseCase;
    private final ConcluirRotaUseCase concluirRotaUseCase;
    private final RecentrarSessaoUseCase recentrarSessaoUseCase;

    @Context
    private UriInfo uriInfo;

    /** Usado pelo Jersey: monta os casos de uso com os DAOs de verdade. */
    public SessaoResource() {
        this(new InicializarSessaoUseCase(new SessaoDao(), new ListaRoteiroDao(), new PontoMapaDao()),
                new ConsultarSessaoUseCase(new SessaoDao(), new ListaRoteiroDao()),
                new ConcluirRotaUseCase(new SessaoDao()),
                new RecentrarSessaoUseCase(new SessaoDao(), new ListaRoteiroDao(), new PontoMapaDao()));
    }

    /** Usado pelos testes, com casos de uso falsos. */
    public SessaoResource(InicializarSessaoUseCase inicializarSessaoUseCase,
                          ConsultarSessaoUseCase consultarSessaoUseCase,
                          ConcluirRotaUseCase concluirRotaUseCase,
                          RecentrarSessaoUseCase recentrarSessaoUseCase) {
        this.inicializarSessaoUseCase = inicializarSessaoUseCase;
        this.consultarSessaoUseCase = consultarSessaoUseCase;
        this.concluirRotaUseCase = concluirRotaUseCase;
        this.recentrarSessaoUseCase = recentrarSessaoUseCase;
    }

    /*
     * O corpo e opcional: sem ele, ou sem o codigo, a sessao nasce sem posicao. Por isso a
     * leitura usa corpoOpcional, e nao corpo.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response inicializar(String corpo) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            IniciarSessaoRequest requisicao =
                    LeituraDaRequisicao.corpoOpcional(corpo, IniciarSessaoRequest.class, path);

            SessaoResponse sessao = inicializarSessaoUseCase.executar(
                    requisicao == null ? null : requisicao.getCodigoPonto());

            URI endereco = uriInfo.getAbsolutePathBuilder().path(sessao.getId().toString()).build();
            return Response.created(endereco).entity(sessao).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    @GET
    @Path("/{sessaoId}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response consultar(@PathParam("sessaoId") String sessaoId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            return Response.ok(consultarSessaoUseCase.executar(sessao)).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    @PUT
    @Path("/{sessaoId}/posicao")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response recentrar(@PathParam("sessaoId") String sessaoId, String corpo) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            RecentrarSessaoRequest requisicao =
                    LeituraDaRequisicao.corpo(corpo, RecentrarSessaoRequest.class, path);

            List<StandardError.ValidationError> erros = requisicao.validar();
            if (!erros.isEmpty()) {
                return RespostaDeErro.validacao(path, erros);
            }
            return Response.ok(
                    recentrarSessaoUseCase.executar(sessao, requisicao.getCodigoPonto())).build();
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
    @Path("/{sessaoId}/concluir")
    @Produces(MediaType.APPLICATION_JSON)
    public Response concluir(@PathParam("sessaoId") String sessaoId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            return Response.ok(concluirRotaUseCase.executar(sessao)).build();
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
