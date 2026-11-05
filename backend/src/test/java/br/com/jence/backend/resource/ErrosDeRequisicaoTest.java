package br.com.jence.backend.resource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Rede de protecao contra um defeito que apareceu tres vezes durante a Fase 1: uma requisicao
 * malformada do cliente caindo no tratamento generico e sendo reportada como falha do servidor.
 * <p>
 * Erro de cliente e 4xx. Se algum caso aqui virar 5xx, o backend esta se acusando de um
 * problema que nao e dele - o que polui monitoramento e impede o frontend de distinguir "eu
 * mandei errado" de "o servidor caiu".
 * <p>
 * Os casos que o Jersey responde antes de chegar ao Resource (405, 415, caminho inexistente)
 * mantem o status, mas vem sem corpo; isso e aceito pelo RF-04.
 */
class ErrosDeRequisicaoTest {

    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(
                new SessaoResource(new SessaoResourceTest.InicializarFalso(),
                        new SessaoResourceTest.ConsultarFalso(),
                        new SessaoResourceTest.ConcluirFalso(),
                        new SessaoResourceTest.RecentrarFalso()),
                new RoteiroResource(new RoteiroResourceTest.ConsultarFalso(),
                        new RoteiroResourceTest.AdicionarFalso(),
                        new RoteiroResourceTest.RemoverFalso()));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    private void assertNaoEErroDeServidor(String cenario, ServidorDeTeste.Resposta resposta, int esperado) {
        System.out.printf(">>> %-42s -> %d%n", cenario, resposta.status());

        assertTrue(resposta.status() < 500, cenario + ": erro de cliente nunca pode virar 5xx");
        assertEquals(esperado, resposta.status(), cenario);
    }

    @Test
    @DisplayName("nenhuma requisicao malformada pode resultar em 5xx")
    void requisicoesMalformadasNaoSaoErroDeServidor() throws Exception {
        UUID id = UUID.randomUUID();
        String itens = "/api/v1/sessoes/" + id + "/roteiro/itens";

        assertNaoEErroDeServidor("UUID malformado no caminho",
                servidor.get("/api/v1/sessoes/nao-e-uuid"), 400);

        assertNaoEErroDeServidor("metodo HTTP nao suportado",
                servidor.delete("/api/v1/sessoes/" + id), 405);

        // Precisa ser um endpoint que consome JSON: onde nao ha corpo, o content-type e
        // irrelevante e responder 200 esta correto.
        assertNaoEErroDeServidor("content-type nao suportado",
                servidor.enviar("POST", itens, "application/xml", "<xml/>"), 415);

        assertNaoEErroDeServidor("corpo JSON malformado",
                servidor.json("POST", itens, "{ nao e json }"), 400);

        assertNaoEErroDeServidor("corpo sem campo obrigatorio",
                servidor.json("POST", itens, "{}"), 400);

        assertNaoEErroDeServidor("caminho inexistente",
                servidor.get("/api/v1/caminho/que/nao/existe"), 404);

        assertNaoEErroDeServidor("caminho inexistente sob recurso valido",
                servidor.get("/api/v1/sessoes/" + id + "/inexistente"), 404);
    }
}
