package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Acoes sobre um item durante a caminhada pela loja.
 * <p>
 * Controller separado do de roteiro porque estas rotas nao levam a sessao no caminho: o
 * celular ja tem o id do item vindo da rota recebida no handoff. O endpoint de ruptura de
 * estoque (Fase 2) compartilha este mesmo prefixo.
 */
@RestController
@RequestMapping("/api/v1/roteiro/itens")
@RequiredArgsConstructor
@Tag(name = "Roteiro", description = "Execucao da rota no celular")
public class ItemRoteiroController {

    private final MarcarItemColetadoUseCase marcarItemColetadoUseCase;

    @PatchMapping("/{itemId}/coletar")
    @Operation(summary = "Marcar item como coletado (UC-014)",
            description = "Acionado quando o cliente confirma ter pego o produto da prateleira. "
                    + "E o que faz o marcador avancar na navegacao e o que indica ao sistema "
                    + "onde o cliente esta. Idempotente.")
    public ResponseEntity<ItemRoteiroDetalhadoResponse> coletar(@PathVariable UUID itemId) {
        return ResponseEntity.ok(marcarItemColetadoUseCase.executar(itemId));
    }
}
