package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.dto.RupturaEstoqueResponse;
import br.com.jence.backend.application.usecase.DesmarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.TratarRupturaEstoqueUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Acoes sobre um item durante a caminhada pela loja.
 * <p>
 * Controller separado do de roteiro porque estas rotas nao levam a sessao no caminho: o
 * celular ja tem o id do item vindo da lista de compras.
 */
@RestController
@RequestMapping("/api/v1/roteiro/itens")
@RequiredArgsConstructor
@Tag(name = "Roteiro", description = "Execucao da rota no celular")
public class ItemRoteiroController {

    private final MarcarItemColetadoUseCase marcarItemColetadoUseCase;
    private final DesmarcarItemColetadoUseCase desmarcarItemColetadoUseCase;
    private final TratarRupturaEstoqueUseCase tratarRupturaEstoqueUseCase;

    @PatchMapping("/{itemId}/coletar")
    @Operation(summary = "Marcar item como coletado (UC-014)",
            description = "Acionado quando o cliente confirma ter pego o produto da prateleira. "
                    + "E o que faz o marcador avancar na navegacao e o que indica ao sistema "
                    + "onde o cliente esta. Idempotente.")
    public ResponseEntity<ItemRoteiroDetalhadoResponse> coletar(@PathVariable UUID itemId) {
        return ResponseEntity.ok(marcarItemColetadoUseCase.executar(itemId));
    }

    @PatchMapping("/{itemId}/desmarcar")
    @Operation(summary = "Desfazer a coleta de um item",
            description = "Acionado quando o cliente tocou por engano ou devolveu o produto a "
                    + "prateleira. A posicao dele volta sozinha para o item marcado antes "
                    + "deste - ou para a placa lida, se nao houver nenhum. Idempotente.")
    public ResponseEntity<ItemRoteiroDetalhadoResponse> desmarcar(@PathVariable UUID itemId) {
        return ResponseEntity.ok(desmarcarItemColetadoUseCase.executar(itemId));
    }

    /*
     * POST e nao PATCH: nao e a atualizacao de um campo do item, e o relato de um fato novo -
     * a gondola estava vazia - que fica registrado no banco. Tambem nao e idempotente: dois
     * toques no botao sao duas visitas frustradas a prateleira, e a loja precisa das duas.
     */
    @PostMapping("/{itemId}/ruptura")
    @Operation(summary = "Relatar prateleira vazia e receber um substituto (UC-013)",
            description = "Acionado quando o cliente chega a prateleira e o produto acabou. "
                    + "Registra a ruptura, filtra no banco os produtos disponiveis fisicamente "
                    + "proximos e aciona o assistente para eleger, entre esses candidatos e "
                    + "somente eles, o que cumpre a mesma funcao. O campo origemSugestao "
                    + "informa se a escolha foi do assistente ou do calculo de proximidade.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Substituto sugerido"),
            @ApiResponse(responseCode = "404", description = "Item de roteiro inexistente"),
            @ApiResponse(responseCode = "409", description = "Sessao encerrada ou expirada"),
            @ApiResponse(responseCode = "422",
                    description = "Ruptura registrada, mas nenhum substituto plausivel por perto")
    })
    public ResponseEntity<RupturaEstoqueResponse> relatarRuptura(@PathVariable UUID itemId) {
        return ResponseEntity.ok(tratarRupturaEstoqueUseCase.executar(itemId));
    }
}
