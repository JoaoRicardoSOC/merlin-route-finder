package br.com.jence.backend.presentation;

import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConcluirRotaUseCase;
import br.com.jence.backend.application.usecase.ConsultarListaRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConsultarSessaoUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.RemoverProdutoDoRoteiroUseCase;
import br.com.jence.backend.presentation.controller.RoteiroController;
import br.com.jence.backend.presentation.controller.SessaoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.RequestBuilder;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

/**
 * Rede de protecao contra um defeito que apareceu tres vezes durante a Fase 1: uma requisicao
 * malformada do cliente caindo no handler generico e sendo reportada como falha do servidor.
 * <p>
 * Erro de cliente e 4xx. Se algum caso aqui virar 5xx, o backend esta se acusando de um
 * problema que nao e dele - o que polui monitoramento e impede o frontend de distinguir "eu
 * mandei errado" de "o servidor caiu".
 */
@WebMvcTest({SessaoController.class, RoteiroController.class})
class ErrosDeRequisicaoTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean InicializarSessaoUseCase inicializarSessaoUseCase;
    @MockitoBean ConsultarSessaoUseCase consultarSessaoUseCase;
    @MockitoBean ConcluirRotaUseCase concluirRotaUseCase;
    @MockitoBean ConsultarListaRoteiroUseCase consultarListaRoteiroUseCase;
    @MockitoBean AdicionarProdutoAoRoteiroUseCase adicionarProdutoAoRoteiroUseCase;
    @MockitoBean RemoverProdutoDoRoteiroUseCase removerProdutoDoRoteiroUseCase;

    private int statusDe(RequestBuilder request) throws Exception {
        MvcResult resultado = mockMvc.perform(request).andReturn();
        return resultado.getResponse().getStatus();
    }

    private void assertNaoEErroDeServidor(String cenario, RequestBuilder request, int esperado) throws Exception {
        int status = statusDe(request);
        System.out.printf(">>> %-42s -> %d%n", cenario, status);

        assertThat(status)
                .as("%s: erro de cliente nunca pode virar 5xx", cenario)
                .isLessThan(500);
        assertThat(status).as("%s", cenario).isEqualTo(esperado);
    }

    @Test
    @DisplayName("nenhuma requisicao malformada pode resultar em 5xx")
    void requisicoesMalformadasNaoSaoErroDeServidor() throws Exception {
        UUID id = UUID.randomUUID();

        assertNaoEErroDeServidor("UUID malformado no caminho",
                get("/api/v1/sessoes/{id}", "nao-e-uuid"), 400);

        assertNaoEErroDeServidor("metodo HTTP nao suportado",
                delete("/api/v1/sessoes/{id}", id), 405);

        // Precisa ser um endpoint que consome JSON: onde nao ha corpo, o content-type e
        // irrelevante e responder 200 esta correto.
        assertNaoEErroDeServidor("content-type nao suportado",
                post("/api/v1/sessoes/{id}/roteiro/itens", id)
                        .contentType(MediaType.APPLICATION_XML).content("<xml/>"), 415);

        assertNaoEErroDeServidor("corpo JSON malformado",
                post("/api/v1/sessoes/{id}/roteiro/itens", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{ nao e json }"), 400);

        // Nenhum endpoint usa parametro de query obrigatorio; o handler de
        // MissingServletRequestParameterException segue no lugar como guarda para os proximos.
        assertNaoEErroDeServidor("corpo sem campo obrigatorio",
                post("/api/v1/sessoes/{id}/roteiro/itens", id)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"), 400);

        assertNaoEErroDeServidor("caminho inexistente",
                get("/api/v1/caminho/que/nao/existe"), 404);

        assertNaoEErroDeServidor("caminho inexistente sob recurso valido",
                get("/api/v1/sessoes/{id}/inexistente", id), 404);
    }
}
