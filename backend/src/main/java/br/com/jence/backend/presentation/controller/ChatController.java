package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.application.dto.EnviarMensagemRequest;
import br.com.jence.backend.application.usecase.ConsultarHistoricoChatUseCase;
import br.com.jence.backend.application.usecase.ConversarComAssistenteUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Conversa do cliente com o assistente de compras (UC-007 a UC-009).
 * <p>
 * Controller proprio, e nao mais um metodo no {@code RoteiroController}: chat e roteiro sao
 * recursos distintos sob a mesma sessao, e o contrato ja os separa em tags diferentes.
 */
@RestController
@RequestMapping("/api/v1/sessoes/{sessaoId}/chat/mensagens")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Assistente virtual de compras")
public class ChatController {

    private final ConversarComAssistenteUseCase conversarComAssistenteUseCase;
    private final ConsultarHistoricoChatUseCase consultarHistoricoChatUseCase;

    @GetMapping
    @Operation(summary = "Consultar historico conversacional da sessao (UC-007 a UC-009)",
            description = "Devolve as mensagens em ordem cronologica, do cliente e do "
                    + "assistente. Sessao encerrada nao impede a leitura: o historico e "
                    + "registro do que ja aconteceu.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Historico da conversa, possivelmente vazio"),
            @ApiResponse(responseCode = "404", description = "Sessao inexistente")
    })
    public ResponseEntity<List<ChatMensagemResponse>> historico(@PathVariable UUID sessaoId) {
        return ResponseEntity.ok(consultarHistoricoChatUseCase.executar(sessaoId));
    }

    /*
     * 201 conforme o contrato: a chamada cria uma mensagem no historico. Sem cabecalho
     * Location, ao contrario dos outros 201 do sistema, porque nao existe endereco para uma
     * mensagem isolada - o recurso enderecavel e o historico inteiro, no GET acima.
     *
     * A resposta e a mensagem do ASSISTENTE, nao a do cliente: e o que o celular precisa
     * exibir. A pergunta ele ja tem em maos.
     */
    @PostMapping
    @Operation(summary = "Enviar mensagem ao assistente virtual (UC-007 a UC-009)",
            description = "O assistente consulta o catalogo real da loja antes de responder e "
                    + "tem escopo fechado: recusa assuntos fora de reforma, construcao, "
                    + "decoracao e jardinagem. Se o assistente estiver indisponivel, a resposta "
                    + "e uma mensagem honesta de indisponibilidade, que nao entra no historico.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Resposta do assistente"),
            @ApiResponse(responseCode = "400", description = "Conteudo vazio ou acima de 1000 caracteres"),
            @ApiResponse(responseCode = "404", description = "Sessao inexistente"),
            @ApiResponse(responseCode = "409", description = "Sessao encerrada ou expirada")
    })
    public ResponseEntity<ChatMensagemResponse> enviar(
            @PathVariable UUID sessaoId,
            @Valid @RequestBody EnviarMensagemRequest request) {

        ChatMensagemResponse resposta =
                conversarComAssistenteUseCase.executar(sessaoId, request.conteudo());

        return ResponseEntity.status(HttpStatus.CREATED).body(resposta);
    }
}
