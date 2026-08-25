package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.MapaResponse;
import br.com.jence.backend.application.usecase.ConsultarMapaUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/mapa")
@RequiredArgsConstructor
@Tag(name = "Mapa", description = "Planta da loja para desenhar a tela de mapa")
public class MapaController {

    private final ConsultarMapaUseCase consultarMapaUseCase;

    @GetMapping
    @Operation(summary = "Consultar a planta da loja",
            description = "Devolve os corredores como retangulos e os pontos de servico, no "
                    + "grid 0-100. Nao depende de sessao: o mapa e o mesmo para todo cliente, "
                    + "e pode ser buscado uma vez e guardado no aparelho.")
    public ResponseEntity<MapaResponse> consultar() {
        return ResponseEntity.ok(consultarMapaUseCase.executar());
    }
}
