package br.com.jence.backend.resource;

import br.com.jence.backend.dto.IniciarSessaoRequest;
import br.com.jence.backend.dto.RecentrarSessaoRequest;
import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.service.ConcluirRotaUseCase;
import br.com.jence.backend.service.ConsultarSessaoUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.RecentrarSessaoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Ciclo de vida da sessao do cliente.
 * <p>
 * Resource fino de proposito: apenas recebe, delega e devolve. As falhas de dominio sao
 * traduzidas em status HTTP pelo {@code GlobalExceptionHandler}, entao nao ha tratamento de
 * erro aqui.
 */
@RestController
@RequestMapping("/api/v1/sessoes")
@Tag(name = "Sessao", description = "Ciclo de vida da sessao do cliente")
public class SessaoResource {

    private final InicializarSessaoUseCase inicializarSessaoUseCase;
    private final ConsultarSessaoUseCase consultarSessaoUseCase;
    private final ConcluirRotaUseCase concluirRotaUseCase;
    private final RecentrarSessaoUseCase recentrarSessaoUseCase;

    public SessaoResource(InicializarSessaoUseCase inicializarSessaoUseCase,
                            ConsultarSessaoUseCase consultarSessaoUseCase,
                            ConcluirRotaUseCase concluirRotaUseCase,
                            RecentrarSessaoUseCase recentrarSessaoUseCase) {
        this.inicializarSessaoUseCase = inicializarSessaoUseCase;
        this.consultarSessaoUseCase = consultarSessaoUseCase;
        this.concluirRotaUseCase = concluirRotaUseCase;
        this.recentrarSessaoUseCase = recentrarSessaoUseCase;
    }

    @PostMapping
    @Operation(summary = "Inicializar sessao (UC-001)",
            description = "Acionado quando o cliente entra pela placa de localizacao, "
                    + "escaneando o QR Code ou digitando o codigo. Cria uma sessao ACTIVE com "
                    + "uma lista de roteiro vazia. O corpo e opcional: sem codigo, ou com um "
                    + "codigo desconhecido, a sessao nasce sem posicao e continua utilizavel.")
    public ResponseEntity<SessaoResponse> inicializar(
            @RequestBody(required = false) IniciarSessaoRequest requisicao) {
        SessaoResponse sessao = inicializarSessaoUseCase.executar(
                requisicao == null ? null : requisicao.codigoPonto());

        return ResponseEntity
                .created(URI.create("/api/v1/sessoes/" + sessao.id()))
                .body(sessao);
    }

    @GetMapping("/{sessaoId}")
    @Operation(summary = "Consultar status da sessao",
            description = "Devolve o status da sessao e onde o cliente esta.")
    public ResponseEntity<SessaoResponse> consultar(@PathVariable UUID sessaoId)
            throws SQLException {
        return ResponseEntity.ok(consultarSessaoUseCase.executar(sessaoId));
    }

    @PutMapping("/{sessaoId}/posicao")
    @Operation(summary = "Recentrar a posicao do cliente",
            description = "Acionado quando o cliente se perde e le outra placa da loja. "
                    + "Atualiza apenas onde ele esta: a lista e o que ja foi coletado "
                    + "permanecem intactos, e nenhuma sessao nova e criada.")
    public ResponseEntity<SessaoResponse> recentrar(
            @PathVariable UUID sessaoId,
            @Valid @RequestBody RecentrarSessaoRequest requisicao) {
        return ResponseEntity.ok(
                recentrarSessaoUseCase.executar(sessaoId, requisicao.codigoPonto()));
    }

    @PostMapping("/{sessaoId}/concluir")
    @Operation(summary = "Encerrar a sessao (UC-014)",
            description = "Acionado quando o cliente finaliza a compra. Nao exige "
                    + "que todos os itens tenham sido coletados.")
    public ResponseEntity<SessaoResponse> concluir(@PathVariable UUID sessaoId) {
        return ResponseEntity.ok(concluirRotaUseCase.executar(sessaoId));
    }
}
