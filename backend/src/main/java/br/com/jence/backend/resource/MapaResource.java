package br.com.jence.backend.resource;

import br.com.jence.backend.dao.PontoMapaDao;
import br.com.jence.backend.service.ConsultarMapaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;

@Path("/api/v1/mapa")
@Tag(name = "Mapa", description = "Planta da loja para desenhar a tela de mapa")
public class MapaResource {

    private final ConsultarMapaUseCase consultarMapaUseCase;

    @Context
    private UriInfo uriInfo;

    /** Usado pelo Jersey: monta o caso de uso com os DAOs de verdade. */
    public MapaResource() {
        this(new ConsultarMapaUseCase(new PontoMapaDao()));
    }

    /** Usado pelos testes, com o caso de uso falso. */
    public MapaResource(ConsultarMapaUseCase consultarMapaUseCase) {
        this.consultarMapaUseCase = consultarMapaUseCase;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Consultar a planta da loja",
            description = "Devolve os corredores como retangulos e os pontos de servico, no "
                    + "grid 0-100. Nao depende de sessao: o mapa e o mesmo para todo cliente, "
                    + "e pode ser buscado uma vez e guardado no aparelho.")
    public Response consultar() {
        try {
            return Response.ok(consultarMapaUseCase.executar()).build();
        } catch (Exception e) {
            return RespostaDeErro.erroInterno(e, uriInfo.getRequestUri().getPath());
        }
    }
}
