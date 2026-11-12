package br.com.jence.backend.resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.sql.SQLRecoverableException;
import java.sql.SQLTimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * O que o cliente ve quando a falha e nossa, e nao dele.
 * <p>
 * Banco fora do ar e 500 legitimo - diferente dos erros de requisicao que o
 * {@link ErrosDeRequisicaoTest} cobre. O que se verifica aqui e outra coisa: que a resposta
 * continua sendo JSON no formato de sempre, e que <b>nada do detalhe interno vaza</b> para
 * quem chamou.
 */
class FalhaDeInfraestruturaTest {

    private final ProdutoResourceTest.BuscarFalso buscar = new ProdutoResourceTest.BuscarFalso();
    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(new ProdutoResource(buscar,
                new ProdutoResourceTest.ConsultarFalso(), new ProdutoResourceTest.SimularFalso(),
                new ProdutoResourceTest.SecoesFalso()));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    private ServidorDeTeste.Resposta quandoOBancoFalhaCom(Exception falha) throws Exception {
        buscar.falha = falha;
        return servidor.get("/api/v1/produtos?query=tinta");
    }

    private void assertRespostaLimpa(ServidorDeTeste.Resposta resposta, String cenario) {
        String corpo = resposta.corpo();

        System.out.printf(">>> %-34s -> HTTP %d%n", cenario, resposta.status());

        assertEquals(500, resposta.status(),
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
        assertFalse(corpoMinusculo.contains("exception"), vazamento);
        assertFalse(corpoMinusculo.contains("br.com.jence"), vazamento);
    }

    @Test
    @DisplayName("SONDA: banco inacessivel devolve erro limpo, sem vazar detalhe interno")
    void bancoInacessivel() throws Exception {
        assertRespostaLimpa(
                quandoOBancoFalhaCom(new SQLRecoverableException(
                        "IO Error: ORA-12170: TNS:Connect timeout occurred")),
                "conexao com o banco indisponivel");
    }

    @Test
    @DisplayName("SONDA: consulta que estoura o tempo limite tambem nao vaza detalhe")
    void consultaLenta() throws Exception {
        assertRespostaLimpa(
                quandoOBancoFalhaCom(new SQLTimeoutException(
                        "ORA-01013: user requested cancel of current operation on tb_ch26_produto")),
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
