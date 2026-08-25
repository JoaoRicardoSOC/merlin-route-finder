package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.dto.PontoMapaResponse;
import br.com.jence.backend.application.dto.ProdutoDetalhadoResponse;
import br.com.jence.backend.application.dto.ListaRoteiroResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.dto.RupturaEstoqueResponse;
import br.com.jence.backend.application.usecase.DesmarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.SubstituirItemDoRoteiroUseCase;
import br.com.jence.backend.application.usecase.TratarRupturaEstoqueUseCase;
import br.com.jence.backend.domain.entity.OrigemSugestao;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.exception.SubstitutoIndisponivelException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemRoteiroController.class)
class ItemRoteiroControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MarcarItemColetadoUseCase marcarItemColetadoUseCase;
    @MockitoBean DesmarcarItemColetadoUseCase desmarcarItemColetadoUseCase;
    @MockitoBean TratarRupturaEstoqueUseCase tratarRupturaEstoqueUseCase;
    @MockitoBean SubstituirItemDoRoteiroUseCase substituirItemDoRoteiroUseCase;

    // ---------------------------------------------------------------- coleta (UC-014)

    @Test
    @DisplayName("PATCH coletar devolve o item marcado")
    void coletarItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        when(marcarItemColetadoUseCase.executar(itemId)).thenReturn(
                new ItemRoteiroDetalhadoResponse(itemId, produtoId, true,
                        new ProdutoResponse(produtoId, "SKU-ENC-001", "Cano PVC Soldavel 25mm 6m", null, null,
                                new BigDecimal("28.90"), 35, UUID.randomUUID())));

        mockMvc.perform(patch("/api/v1/roteiro/itens/{i}/coletar", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.coletado").value(true))
                .andExpect(jsonPath("$.produto.nome").value("Cano PVC Soldavel 25mm 6m"));
    }

    @Test
    @DisplayName("PATCH desmarcar devolve o item sem coleta")
    void desmarcarItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        when(desmarcarItemColetadoUseCase.executar(itemId)).thenReturn(
                new ItemRoteiroDetalhadoResponse(itemId, produtoId, false,
                        new ProdutoResponse(produtoId, "SKU-ENC-001", "Cano PVC Soldavel 25mm 6m",
                                null, null, new BigDecimal("28.90"), 35, UUID.randomUUID())));

        mockMvc.perform(patch("/api/v1/roteiro/itens/{i}/desmarcar", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.coletado").value(false));

        verify(desmarcarItemColetadoUseCase).executar(itemId);
    }

    @Test
    @DisplayName("desmarcar item inexistente devolve 404")
    void desmarcarItemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(desmarcarItemColetadoUseCase.executar(itemId))
                .thenThrow(new RecursoNaoEncontradoException("Item do roteiro", itemId));

        mockMvc.perform(patch("/api/v1/roteiro/itens/{i}/desmarcar", itemId))
                .andExpect(status().isNotFound());
    }

    // ---------------------------------------------------------------- aceitar o substituto

    @Test
    @DisplayName("POST substituir devolve a lista ja com a troca feita")
    void substituirItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID substitutoId = UUID.randomUUID();
        UUID novoItemId = UUID.randomUUID();

        when(substituirItemDoRoteiroUseCase.executar(itemId, substitutoId)).thenReturn(
                new ListaRoteiroResponse(UUID.randomUUID(), UUID.randomUUID(), 1, List.of(
                        new ItemRoteiroDetalhadoResponse(novoItemId, substitutoId, false,
                                new ProdutoResponse(substitutoId, "SKU-TIN-004",
                                        "Lixa d'Agua Grao 150", null, null,
                                        new BigDecimal("4.20"), 40, UUID.randomUUID())))));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/substituir", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoSubstitutoId\":\"" + substitutoId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeItens").value(1))
                .andExpect(jsonPath("$.itens[0].produto.sku").value("SKU-TIN-004"))
                .andExpect(jsonPath("$.itens[0].coletado")
                        .value(false));

        verify(substituirItemDoRoteiroUseCase).executar(itemId, substitutoId);
    }

    @Test
    @DisplayName("substituir sem o produto no corpo devolve 400 apontando o campo")
    void substituirSemProduto() throws Exception {
        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/substituir", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors[0].field").value("produtoSubstitutoId"));
    }

    @Test
    @DisplayName("trocar um produto por ele mesmo devolve 409")
    void substituirPorSiMesmo() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        when(substituirItemDoRoteiroUseCase.executar(any(), any()))
                .thenThrow(new OperacaoNaoPermitidaException(
                        "Produto %s nao pode substituir a si mesmo".formatted(produtoId)));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/substituir", itemId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"produtoSubstitutoId\":\"" + produtoId + "\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("item inexistente devolve 404")
    void itemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(marcarItemColetadoUseCase.executar(any()))
                .thenThrow(new RecursoNaoEncontradoException("Item do roteiro", itemId));

        mockMvc.perform(patch("/api/v1/roteiro/itens/{i}/coletar", itemId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Recurso Nao Encontrado"));
    }

    @Test
    @DisplayName("id malformado devolve 400")
    void idMalformado() throws Exception {
        mockMvc.perform(patch("/api/v1/roteiro/itens/{i}/coletar", "abc"))
                .andExpect(status().isBadRequest());
    }

    // ---------------------------------------------------------------- ruptura (UC-013)

    private RupturaEstoqueResponse sugestaoDaIA(UUID produtoOriginalId) {
        UUID sugeridoId = UUID.randomUUID();
        return new RupturaEstoqueResponse(
                produtoOriginalId,
                sugeridoId,
                new ProdutoDetalhadoResponse(sugeridoId, "SKU-TIN-004", "Lixa d'Agua Grao 150", null, null,
                        new BigDecimal("4.20"), 40, UUID.randomUUID(),
                        new PontoMapaResponse(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", null, 32, 10),
                        List.of()),
                "A lixa d'agua grao 150 da o mesmo acabamento e esta no mesmo corredor.",
                OrigemSugestao.ASSISTENTE_IA);
    }

    @Test
    @DisplayName("POST ruptura devolve o substituto com o corredor onde encontra-lo")
    void relatarRuptura() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID produtoOriginalId = UUID.randomUUID();
        when(tratarRupturaEstoqueUseCase.executar(itemId)).thenReturn(sugestaoDaIA(produtoOriginalId));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/ruptura", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.produtoOriginalId").value(produtoOriginalId.toString()))
                .andExpect(jsonPath("$.produtoSugerido.sku").value("SKU-TIN-004"))
                .andExpect(jsonPath("$.produtoSugerido.saldoEstoque").value(40))
                .andExpect(jsonPath("$.produtoSugerido.pontoMapa.corredor").value("Tintas"))
                .andExpect(jsonPath("$.justificativa").value(org.hamcrest.Matchers.containsString("acabamento")))
                .andExpect(jsonPath("$.origemSugestao").value("ASSISTENTE_IA"));
    }

    @Test
    @DisplayName("a origem da sugestao chega ao frontend, que nao pode rotular fallback como IA")
    void expoeAOrigemDaSugestao() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID sugeridoId = UUID.randomUUID();
        when(tratarRupturaEstoqueUseCase.executar(itemId)).thenReturn(new RupturaEstoqueResponse(
                UUID.randomUUID(), sugeridoId,
                new ProdutoDetalhadoResponse(sugeridoId, "SKU-TIN-002", "Rolo de La 23cm com Cabo", null, null,
                        new BigDecimal("34.90"), 25, UUID.randomUUID(),
                        new PontoMapaResponse(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", null, 32, 10),
                        List.of()),
                "Este e o produto disponivel mais proximo de onde voce esta.",
                OrigemSugestao.PROXIMIDADE));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/ruptura", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.origemSugestao").value("PROXIMIDADE"));
    }

    @Test
    @DisplayName("sem substituto plausivel devolve 422, distinguivel de 404")
    void semSubstitutoPlausivel() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(tratarRupturaEstoqueUseCase.executar(itemId)).thenThrow(
                new SubstitutoIndisponivelException(
                        "Nenhum substituto plausivel para 'Espelho Redondo 60cm' foi encontrado."));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/ruptura", itemId))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Substituto Indisponivel"))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("Espelho Redondo 60cm")));
    }

    @Test
    @DisplayName("sessao encerrada devolve 409")
    void sessaoInativa() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(tratarRupturaEstoqueUseCase.executar(itemId))
                .thenThrow(new OperacaoNaoPermitidaException("Sessao nao esta mais ativa (status EXPIRED)"));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/ruptura", itemId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Operacao Nao Permitida"));
    }

    @Test
    @DisplayName("item inexistente na ruptura devolve 404")
    void rupturaDeItemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        when(tratarRupturaEstoqueUseCase.executar(itemId))
                .thenThrow(new RecursoNaoEncontradoException("Item de roteiro", itemId));

        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/ruptura", itemId))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("id malformado na ruptura devolve 400, nao 500")
    void rupturaComIdMalformado() throws Exception {
        mockMvc.perform(post("/api/v1/roteiro/itens/{i}/ruptura", "nao-e-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Parametro Invalido"));
    }

    @Test
    @DisplayName("GET no endereco da ruptura devolve 405, nao 500")
    void metodoErradoNaRuptura() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/v1/roteiro/itens/{i}/ruptura", UUID.randomUUID()))
                .andExpect(status().isMethodNotAllowed());
    }
}
