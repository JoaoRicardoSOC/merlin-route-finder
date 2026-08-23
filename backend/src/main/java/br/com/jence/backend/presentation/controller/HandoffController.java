package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.HandoffRequest;
import br.com.jence.backend.application.dto.HandoffResponse;
import br.com.jence.backend.application.dto.RotaCalculadaResponse;
import br.com.jence.backend.application.dto.ValidarHandoffRequest;
import br.com.jence.backend.application.usecase.GerarHandoffUseCase;
import br.com.jence.backend.application.usecase.ValidarHandoffUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
    @Operation(summary = "Gerar ou regerar handoff (UC-010)",
            description = "Calcula a rota, assina o token e devolve a URL que o Totem exibe "
                    + "como QR Code. O token vale 5 minutos e so pode ser usado uma vez. "
                    + "Chamar de novo para a mesma sessao REGENERA o QR Code: o token anterior "
                    + "para de valer e a jornada continua de onde estava, sem reiniciar a "
                    + "montagem da lista. Se a caminhada ja tiver comecado, a ordem das paradas "
                    + "e preservada.")
    public ResponseEntity<HandoffResponse> gerar(@Valid @RequestBody HandoffRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(gerarHandoffUseCase.executar(request.sessaoId()));
    }

    /*
     * POST com o token no corpo, e nao mais GET com ele na query string. Duas razoes que se
     * somam: URL fica gravada em historico de navegador e em log de servidor, e esta operacao
     * CONSOME o token e renova a sessao - efeitos colaterais que um GET nunca deveria ter.
     * Ver D-44.
     */
    @PostMapping("/validate")
    @Operation(summary = "Validar token e iniciar navegacao (UC-011)",
            description = "Acionado quando o celular escaneia o QR Code. Consome o token e "
                    + "devolve a rota com itens e coordenadas. O token vai no corpo da "
                    + "requisicao, nunca na URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Rota liberada para o celular"),
            @ApiResponse(responseCode = "400", description = "Token ausente ou em branco"),
            @ApiResponse(responseCode = "401",
                    description = "Token invalido, ja utilizado ou expirado. Quando o rotulo do "
                            + "erro for 'Token de Handoff Expirado', o Totem pode gerar um QR "
                            + "Code novo para a mesma sessao em vez de reiniciar a jornada."),
            @ApiResponse(responseCode = "409", description = "Sessao encerrada ou expirada")
    })
    public ResponseEntity<RotaCalculadaResponse> validar(@Valid @RequestBody ValidarHandoffRequest request) {
        return ResponseEntity.ok(validarHandoffUseCase.executar(request.token()));
    }
}
