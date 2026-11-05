package br.com.jence.backend.resource;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Responde se a aplicacao esta no ar, para o health check do Render.
 * <p>
 * Nao toca no banco de proposito: o Render chama este endereco a cada poucos segundos, e cada
 * chamada gastaria uma conexao com o Oracle. Se o banco cair, as requisicoes de negocio falham
 * com 503 por conta propria; reiniciar a aplicacao nao traria o banco de volta.
 */
@Path("/health")
public class HealthResource {

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response verificar() {
        return Response.ok("OK").build();
    }
}
