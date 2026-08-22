package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.PaginaResponse;
import br.com.jence.backend.application.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.usecase.BuscarProdutosUseCase;
import br.com.jence.backend.application.usecase.ConsultarProdutoUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Consulta do catalogo de produtos.
 * <p>
 * Segue o padrao do {@code SessaoController}: apenas recebe, delega e devolve. Os limites de
 * paginacao e o tratamento de termo em branco ficam no caso de uso, que ja e dono dessa regra.
 */
@RestController
@RequestMapping("/api/v1/produtos")
@RequiredArgsConstructor
@Tag(name = "Catalogo", description = "Busca e detalhamento de produtos")
public class ProdutoController {

    private final BuscarProdutosUseCase buscarProdutosUseCase;
    private final ConsultarProdutoUseCase consultarProdutoUseCase;

    @GetMapping
    @Operation(summary = "Busca paginada e fuzzy de produtos (UC-002)",
            description = "Sem termo, navega o catalogo. Com termo, filtra por nome tolerando "
                    + "busca parcial e pequenos erros de digitacao.")
    public ResponseEntity<PaginaResponse<ProdutoResponse>> buscar(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {

        // 'query' e o nome publico no contrato; 'termo' e o vocabulario interno.
        return ResponseEntity.ok(buscarProdutosUseCase.executar(query, page, size));
    }

    @GetMapping("/{produtoId}")
    @Operation(summary = "Detalhamento de produto (UC-003)",
            description = "Inclui o ponto de mapa onde o produto esta fisicamente na loja.")
    public ResponseEntity<ProdutoDetalhadoResponse> consultar(@PathVariable UUID produtoId) {
        return ResponseEntity.ok(consultarProdutoUseCase.executar(produtoId));
    }
}
