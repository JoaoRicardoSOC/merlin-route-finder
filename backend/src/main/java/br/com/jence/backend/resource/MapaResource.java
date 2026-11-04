package br.com.jence.backend.resource;

import br.com.jence.backend.dto.MapaResponse;
import br.com.jence.backend.service.ConsultarMapaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/v1/mapa")
@Tag(name = "Mapa", description = "Planta da loja para desenhar a tela de mapa")
public class MapaResource {

    private final ConsultarMapaUseCase consultarMapaUseCase;

    public MapaResource(ConsultarMapaUseCase consultarMapaUseCase) {
        this.consultarMapaUseCase = consultarMapaUseCase;
    }

    @GetMapping
    @Operation(summary = "Consultar a planta da loja",
            description = "Devolve os corredores como retangulos e os pontos de servico, no "
                    + "grid 0-100. Nao depende de sessao: o mapa e o mesmo para todo cliente, "
                    + "e pode ser buscado uma vez e guardado no aparelho.")
    public ResponseEntity<MapaResponse> consultar() throws SQLException {
        return ResponseEntity.ok(consultarMapaUseCase.executar());
    }
}
