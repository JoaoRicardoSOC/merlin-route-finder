package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.HandoffRequest;
import br.com.jence.backend.application.dto.HandoffResponse;
import br.com.jence.backend.application.dto.RotaCalculadaResponse;
import br.com.jence.backend.application.usecase.GerarHandoffUseCase;
import br.com.jence.backend.application.usecase.ValidarHandoffUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transicao do roteiro do Totem para o celular do cliente.
 */
@RestController
@RequestMapping("/api/v1/handoff")
@RequiredArgsConstructor
@Tag(name = "Handoff", description = "Transicao de dispositivo Totem -> Mobile via QR Code")
public class HandoffController {

    private final GerarHandoffUseCase gerarHandoffUseCase;
    private final ValidarHandoffUseCase validarHandoffUseCase;

    @PostMapping
    @Operation(summary = "Gerar handoff (UC-010)",
            description = "Calcula a rota, assina o token e devolve a URL que o Totem exibe "
                    + "como QR Code. O token vale 5 minutos e so pode ser usado uma vez.")
    public ResponseEntity<HandoffResponse> gerar(@Valid @RequestBody HandoffRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(gerarHandoffUseCase.executar(request.sessaoId()));
    }

    /*
     * O token vai na query string porque e assim que o diagrama de sequencia e o contrato
     * definem. Tirar dali faz parte do hardening previsto para a Fase 3 (ver D-29).
     */
    @GetMapping("/validate")
    @Operation(summary = "Validar token e iniciar navegacao (UC-011)",
            description = "Acionado quando o celular escaneia o QR Code. Consome o token e "
                    + "devolve a rota com itens e coordenadas.")
    public ResponseEntity<RotaCalculadaResponse> validar(@RequestParam String token) {
        return ResponseEntity.ok(validarHandoffUseCase.executar(token));
    }
}
