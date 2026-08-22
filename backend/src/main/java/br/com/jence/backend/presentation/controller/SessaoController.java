package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.application.usecase.ConcluirRotaUseCase;
import br.com.jence.backend.application.usecase.ConsultarSessaoUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.UUID;

/**
 * Ciclo de vida da sessao do cliente.
 * <p>
 * Controller fino de proposito: apenas recebe, delega e devolve. As falhas de dominio sao
 * traduzidas em status HTTP pelo {@code GlobalExceptionHandler}, entao nao ha tratamento de
 * erro aqui.
 */
@RestController
@RequestMapping("/api/v1/sessoes")
@RequiredArgsConstructor
@Tag(name = "Sessao", description = "Ciclo de vida da sessao do cliente (Totem e Mobile)")
public class SessaoController {

    private final InicializarSessaoUseCase inicializarSessaoUseCase;
    private final ConsultarSessaoUseCase consultarSessaoUseCase;
    private final ConcluirRotaUseCase concluirRotaUseCase;

    @PostMapping
    @Operation(summary = "Inicializar sessao (UC-001)",
            description = "Acionado quando o cliente inicia a interacao no Totem. "
                    + "Cria uma sessao ACTIVE com uma lista de roteiro vazia.")
    public ResponseEntity<SessaoResponse> inicializar() {
        SessaoResponse sessao = inicializarSessaoUseCase.executar();

        return ResponseEntity
                .created(URI.create("/api/v1/sessoes/" + sessao.id()))
                .body(sessao);
    }

    @GetMapping("/{sessaoId}")
    @Operation(summary = "Consultar status da sessao",
            description = "Permite ao Totem ou ao celular verificar se a sessao ainda esta ativa.")
    public ResponseEntity<SessaoResponse> consultar(@PathVariable UUID sessaoId) {
        return ResponseEntity.ok(consultarSessaoUseCase.executar(sessaoId));
    }

    @PostMapping("/{sessaoId}/concluir")
    @Operation(summary = "Concluir rota e encerrar sessao (UC-014)",
            description = "Acionado pelo celular quando o cliente finaliza a caminhada. Nao exige "
                    + "que todos os itens tenham sido coletados.")
    public ResponseEntity<SessaoResponse> concluir(@PathVariable UUID sessaoId) {
        return ResponseEntity.ok(concluirRotaUseCase.executar(sessaoId));
    }
}
