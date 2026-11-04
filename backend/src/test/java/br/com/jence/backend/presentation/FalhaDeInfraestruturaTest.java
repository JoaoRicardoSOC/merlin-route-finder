package br.com.jence.backend.presentation;

import br.com.jence.backend.application.usecase.BuscarProdutosUseCase;
import br.com.jence.backend.application.usecase.ConsultarProdutoUseCase;
import br.com.jence.backend.application.usecase.ListarSecoesUseCase;
import br.com.jence.backend.application.usecase.SimularEstoqueUseCase;
import br.com.jence.backend.presentation.controller.ProdutoController;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * O que o cliente ve quando a falha e nossa, e nao dele.
 * <p>
 * Banco fora do ar e 500 legitimo - diferente dos erros de requisicao que o
 * {@link ErrosDeRequisicaoTest} cobre. O que se verifica aqui e outra coisa: que a resposta
 * continua sendo JSON no formato de sempre, e que <b>nada do detalhe interno vaza</b> para
 * quem chamou.
 */
@WebMvcTest(ProdutoController.class)
class FalhaDeInfraestruturaTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean BuscarProdutosUseCase buscarProdutosUseCase;
    @MockitoBean ConsultarProdutoUseCase consultarProdutoUseCase;
    @MockitoBean SimularEstoqueUseCase simularEstoqueUseCase;
    @MockitoBean ListarSecoesUseCase listarSecoesUseCase;

    private MvcResult quandoOBancoFalhaCom(RuntimeException falha) throws Exception {
        when(buscarProdutosUseCase.executar(any(), any(), any(), any(), any(), any())).thenThrow(falha);
        return mockMvc.perform(get("/api/v1/produtos").param("query", "tinta")).andReturn();
    }

    private void assertRespostaLimpa(MvcResult resultado, String cenario) throws Exception {
        String corpo = resultado.getResponse().getContentAsString();

        System.out.printf(">>> %-34s -> HTTP %d%n", cenario, resultado.getResponse().getStatus());

        assertEquals(500, resultado.getResponse().getStatus(),
                cenario + ": falha de infraestrutura e responsabilidade do servidor");

        String formato = cenario + ": a resposta precisa continuar sendo o JSON de erro de sempre";
        assertTrue(corpo.contains("\"status\":500"), formato);
        assertTrue(corpo.contains("\"error\":\"Erro Interno do Servidor\""), formato);
        assertTrue(corpo.contains("\"path\":\"/api/v1/produtos\""), formato);

        String vazamento = cenario + ": detalhe interno nao pode vazar para o cliente";
        String corpoMinusculo = corpo.toLowerCase();
        assertFalse(corpoMinusculo.contains("oracle"), vazamento);
        assertFalse(corpoMinusculo.contains("jdbc"), vazamento);
        assertFalse(corpoMinusculo.contains("sql"), vazamento);
        assertFalse(corpoMinusculo.contains("hikari"), vazamento);
        assertFalse(corpoMinusculo.contains("exception"), vazamento);
        assertFalse(corpoMinusculo.contains("br.com.jence"), vazamento);
    }

    @Test
    @DisplayName("SONDA: banco inacessivel devolve erro limpo, sem vazar detalhe interno")
    void bancoInacessivel() throws Exception {
        assertRespostaLimpa(
                quandoOBancoFalhaCom(new CannotGetJdbcConnectionException(
                        "Failed to obtain JDBC Connection: ORA-12170: TNS:Connect timeout occurred")),
                "conexao com o banco indisponivel");
    }

    @Test
    @DisplayName("SONDA: consulta que estoura o tempo limite tambem nao vaza detalhe")
    void consultaLenta() throws Exception {
        assertRespostaLimpa(
                quandoOBancoFalhaCom(new QueryTimeoutException(
                        "Statement cancelled due to timeout on tb_produto")),
                "consulta excedeu o tempo limite");
    }

    @Test
    @DisplayName("SONDA: erro inesperado no caso de uso nao expoe a classe nem a mensagem")
    void erroInesperado() throws Exception {
        assertRespostaLimpa(
                quandoOBancoFalhaCom(new IllegalStateException(
                        "br.com.jence.backend.detalhe.interno explodiu")),
                "erro inesperado");
    }
}
