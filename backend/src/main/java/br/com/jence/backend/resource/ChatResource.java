package br.com.jence.backend.resource;

import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.dao.ProdutoDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.dto.EnviarMensagemRequest;
import br.com.jence.backend.dto.StandardError;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.ia.GeminiClient;
import br.com.jence.backend.resource.LeituraDaRequisicao.RequisicaoInvalida;
import br.com.jence.backend.service.ConsultarHistoricoChatUseCase;
import br.com.jence.backend.service.ConversarComAssistenteUseCase;
import jakarta.ws.rs.Consumes;
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
 * Conversa do cliente com o assistente de compras (UC-007 a UC-009).
 * <p>
 * Resource proprio, e nao mais um metodo no {@code RoteiroResource}: chat e roteiro sao
 * recursos distintos sob a mesma sessao, e o contrato ja os separa em tags diferentes.
 */
@Path("/api/v1/sessoes/{sessaoId}/chat/mensagens")
public class ChatResource {

    /*
     * Uma instancia so para todas as requisicoes. O Jersey cria um Resource novo a cada
     * chamada; sem o static, cada mensagem abriria um HttpClient novo para o Gemini, em vez de
     * reaproveitar as conexoes.
     */
    private static final GeminiClient GEMINI = new GeminiClient();

    private final ConversarComAssistenteUseCase conversarComAssistenteUseCase;
    private final ConsultarHistoricoChatUseCase consultarHistoricoChatUseCase;

    @Context
    private UriInfo uriInfo;

    /** Usado pelo Jersey: monta os casos de uso com os DAOs e o cliente do Gemini de verdade. */
    public ChatResource() {
        this(new ConversarComAssistenteUseCase(new SessaoDao(), new ChatMensagemDao(),
                        new ProdutoDao(), GEMINI),
                new ConsultarHistoricoChatUseCase(new SessaoDao(), new ChatMensagemDao()));
    }

    /** Usado pelos testes, com casos de uso falsos. */
    public ChatResource(ConversarComAssistenteUseCase conversarComAssistenteUseCase,
                        ConsultarHistoricoChatUseCase consultarHistoricoChatUseCase) {
        this.conversarComAssistenteUseCase = conversarComAssistenteUseCase;
        this.consultarHistoricoChatUseCase = consultarHistoricoChatUseCase;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response historico(@PathParam("sessaoId") String sessaoId) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            List<ChatMensagemResponse> mensagens = consultarHistoricoChatUseCase.executar(sessao);
            return Response.ok(mensagens).build();
        } catch (RequisicaoInvalida e) {
            return e.getResposta();
        } catch (RecursoNaoEncontradoException e) {
            return RespostaDeErro.recursoNaoEncontrado(e, path);
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, path);
        }
    }

    /*
     * 201 conforme o contrato: a chamada cria uma mensagem no historico. Sem cabecalho
     * Location, ao contrario dos outros 201 do sistema, porque nao existe endereco para uma
     * mensagem isolada - o recurso enderecavel e o historico inteiro, no GET acima.
     *
     * A resposta e a mensagem do ASSISTENTE, nao a do cliente: e o que o celular precisa
     * exibir. A pergunta ele ja tem em maos.
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response enviar(@PathParam("sessaoId") String sessaoId, String corpo) {
        String path = uriInfo.getRequestUri().getPath();
        try {
            UUID sessao = LeituraDaRequisicao.uuid("sessaoId", sessaoId, path);
            EnviarMensagemRequest request =
                    LeituraDaRequisicao.corpo(corpo, EnviarMensagemRequest.class, path);

            List<StandardError.ValidationError> erros = request.validar();
            if (!erros.isEmpty()) {
                return RespostaDeErro.validacao(path, erros);
            }

            ChatMensagemResponse resposta =
                    conversarComAssistenteUseCase.executar(sessao, request.getConteudo());

            return Response.status(Response.Status.CREATED).entity(resposta).build();
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
