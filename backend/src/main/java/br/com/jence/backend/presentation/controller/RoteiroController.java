package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.AdicionarItemRequest;
import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.dto.ListaRoteiroResponse;
import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConsultarListaRoteiroUseCase;
import br.com.jence.backend.application.usecase.RemoverProdutoDoRoteiroUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Montagem do carrinho de roteiro no Totem.
 * <p>
 * Controller separado do de sessao apesar de compartilhar o prefixo da URL: o recurso e outro
 * e tem casos de uso proprios.
 */
@RestController
@RequestMapping("/api/v1/sessoes/{sessaoId}/roteiro")
@RequiredArgsConstructor
@Tag(name = "Roteiro", description = "Montagem da lista de compras no Totem")
public class RoteiroController {

    private final ConsultarListaRoteiroUseCase consultarListaRoteiroUseCase;
    private final AdicionarProdutoAoRoteiroUseCase adicionarProdutoAoRoteiroUseCase;
    private final RemoverProdutoDoRoteiroUseCase removerProdutoDoRoteiroUseCase;

    @GetMapping
    @Operation(summary = "Consultar a lista de compras da sessao (UC-005)")
    public ResponseEntity<ListaRoteiroResponse> consultar(@PathVariable UUID sessaoId) {
        return ResponseEntity.ok(consultarListaRoteiroUseCase.executar(sessaoId));
    }

    /*
     * Sem header Location de proposito: ele deveria apontar para uma URL que responde GET, e
     * /roteiro/itens/{itemId} so aceita DELETE. O item criado vai no corpo, que e o que o
     * Totem precisa para atualizar a tela.
     */
    @PostMapping("/itens")
    @Operation(summary = "Adicionar produto ao roteiro (UC-004)",
            description = "Sem limite de itens. Produto ja presente devolve o item existente, "
                    + "sem duplicar a parada na rota.")
    public ResponseEntity<ItemRoteiroDetalhadoResponse> adicionar(
            @PathVariable UUID sessaoId,
            @Valid @RequestBody AdicionarItemRequest request) {

        ItemRoteiroDetalhadoResponse item =
                adicionarProdutoAoRoteiroUseCase.executar(sessaoId, request.produtoId());

        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @DeleteMapping("/itens/{itemId}")
    @Operation(summary = "Remover produto da lista (UC-006)")
    public ResponseEntity<Void> remover(@PathVariable UUID sessaoId, @PathVariable UUID itemId) {
        removerProdutoDoRoteiroUseCase.executar(sessaoId, itemId);
        return ResponseEntity.noContent().build();
    }
}
