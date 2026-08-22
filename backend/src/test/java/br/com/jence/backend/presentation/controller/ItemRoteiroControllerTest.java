package br.com.jence.backend.presentation.controller;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.application.dto.ProdutoResponse;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ItemRoteiroController.class)
class ItemRoteiroControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean MarcarItemColetadoUseCase marcarItemColetadoUseCase;

    @Test
    @DisplayName("PATCH coletar devolve o item marcado")
    void coletarItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID produtoId = UUID.randomUUID();
        when(marcarItemColetadoUseCase.executar(itemId)).thenReturn(
                new ItemRoteiroDetalhadoResponse(itemId, produtoId, 2, true,
                        new ProdutoResponse(produtoId, "SKU-ENC-001", "Cano PVC Soldavel 25mm 6m",
                                new BigDecimal("28.90"), 35, UUID.randomUUID())));

        mockMvc.perform(patch("/api/v1/roteiro/itens/{i}/coletar", itemId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(itemId.toString()))
                .andExpect(jsonPath("$.coletado").value(true))
                .andExpect(jsonPath("$.ordemCaminho").value(2))
                .andExpect(jsonPath("$.produto.nome").value("Cano PVC Soldavel 25mm 6m"));
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
}
