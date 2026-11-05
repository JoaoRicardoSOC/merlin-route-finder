package br.com.jence.backend.resource;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.model.StatusSessao;
import br.com.jence.backend.service.ConcluirRotaUseCase;
import br.com.jence.backend.service.ConsultarSessaoUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.service.RecentrarSessaoUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As rotas da sessao pelo Jersey, com casos de uso falsos. */
class SessaoResourceTest {

    static class InicializarFalso extends InicializarSessaoUseCase {
        final List<String> codigos = new ArrayList<>();
        SessaoResponse sessao;

        InicializarFalso() {
            super(null, null, null);
        }

        @Override
        public SessaoResponse executar(String codigoPonto) {
            codigos.add(codigoPonto);
            return sessao;
        }
    }

    static class ConsultarFalso extends ConsultarSessaoUseCase {
        SessaoResponse sessao;
        RecursoNaoEncontradoException falha;

        ConsultarFalso() {
            super(null, null);
        }

        @Override
        public SessaoResponse executar(UUID sessaoId) throws RecursoNaoEncontradoException {
            if (falha != null) {
                throw falha;
            }
            return sessao;
        }
    }

    static class ConcluirFalso extends ConcluirRotaUseCase {
        SessaoResponse sessao;
        OperacaoNaoPermitidaException falha;

        ConcluirFalso() {
            super(null);
        }

        @Override
        public SessaoResponse executar(UUID sessaoId) throws OperacaoNaoPermitidaException {
            if (falha != null) {
                throw falha;
            }
            return sessao;
        }
    }

    static class RecentrarFalso extends RecentrarSessaoUseCase {
        final List<String> codigos = new ArrayList<>();
        SessaoResponse sessao;
        RecursoNaoEncontradoException falha;

        RecentrarFalso() {
            super(null, null, null);
        }

        @Override
        public SessaoResponse executar(UUID sessaoId, String codigoPonto) throws RecursoNaoEncontradoException {
            codigos.add(codigoPonto);
            if (falha != null) {
                throw falha;
            }
            return sessao;
        }
    }

    private final InicializarFalso inicializar = new InicializarFalso();
    private final ConsultarFalso consultar = new ConsultarFalso();
    private final ConcluirFalso concluir = new ConcluirFalso();
    private final RecentrarFalso recentrar = new RecentrarFalso();
    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(new SessaoResource(inicializar, consultar, concluir, recentrar));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    static SessaoResponse sessao(UUID id, StatusSessao status) {
        LocalDateTime agora = LocalDateTime.now();
        return SessaoResponse.de(Sessao.reconstituir(
                id, status, agora, agora.plusMinutes(30), null, null));
    }

    @Test
    @DisplayName("POST /sessoes sem corpo devolve 201 com Location e corpo da sessao")
    void criarSessao() throws Exception {
        UUID id = UUID.randomUUID();
        inicializar.sessao = sessao(id, StatusSessao.ACTIVE);

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", "/api/v1/sessoes");

        assertEquals(201, resposta.status());
        assertTrue(resposta.location().orElseThrow().endsWith("/api/v1/sessoes/" + id));
        assertEquals(id.toString(), resposta.texto("id"));
        assertEquals("ACTIVE", resposta.texto("status"));
        assertNotNull(resposta.json("criadoEm"));
        assertNotNull(resposta.json("expiracaoTtl"));
        assertNull(inicializar.codigos.get(0), "sem corpo, a sessao nasce sem placa");
    }

    @Test
    @DisplayName("POST /sessoes com o codigo da placa repassa o codigo")
    void criarSessaoComPlaca() throws Exception {
        inicializar.sessao = sessao(UUID.randomUUID(), StatusSessao.ACTIVE);

        ServidorDeTeste.Resposta resposta =
                servidor.json("POST", "/api/v1/sessoes", "{\"codigoPonto\":\"TIN-02\"}");

        assertEquals(201, resposta.status());
        assertEquals("TIN-02", inicializar.codigos.get(0));
    }

    @Test
    @DisplayName("GET /sessoes/{id} devolve 200 no formato do contrato")
    void consultarSessao() throws Exception {
        UUID id = UUID.randomUUID();
        consultar.sessao = sessao(id, StatusSessao.ACTIVE);

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/sessoes/" + id);

        assertEquals(200, resposta.status());
        assertEquals(id.toString(), resposta.texto("id"));
        assertEquals("ACTIVE", resposta.texto("status"));
    }

    @Test
    @DisplayName("sessao inexistente vira 404 com o corpo de erro padrao")
    void sessaoInexistente() throws Exception {
        UUID id = UUID.randomUUID();
        consultar.falha = new RecursoNaoEncontradoException("Sessao", id);

        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/sessoes/" + id);

        assertEquals(404, resposta.status());
        assertEquals(404, resposta.inteiro("status"));
        assertEquals("Recurso Não Encontrado", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("Sessao"));
        assertEquals("/api/v1/sessoes/" + id, resposta.texto("path"));
        assertNotNull(resposta.json("timestamp"));
    }

    @Test
    @DisplayName("POST concluir devolve a sessao COMPLETED")
    void concluirRota() throws Exception {
        UUID id = UUID.randomUUID();
        concluir.sessao = sessao(id, StatusSessao.COMPLETED);

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", "/api/v1/sessoes/" + id + "/concluir");

        assertEquals(200, resposta.status());
        assertEquals(id.toString(), resposta.texto("id"));
        assertEquals("COMPLETED", resposta.texto("status"));
    }

    @Test
    @DisplayName("concluir sessao ja encerrada devolve 409")
    void concluirSessaoJaEncerrada() throws Exception {
        UUID id = UUID.randomUUID();
        concluir.falha = new OperacaoNaoPermitidaException(
                "Sessao %s nao esta mais ativa (status COMPLETED)".formatted(id));

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", "/api/v1/sessoes/" + id + "/concluir");

        assertEquals(409, resposta.status());
        assertEquals("Operação Não Permitida", resposta.texto("error"));
    }

    // ---------------------------------------------------------------- recentrar

    @Test
    @DisplayName("PUT posicao devolve a sessao com a nova posicao")
    void recentrar() throws Exception {
        UUID id = UUID.randomUUID();
        recentrar.sessao = sessao(id, StatusSessao.ACTIVE);

        ServidorDeTeste.Resposta resposta = servidor.json("PUT",
                "/api/v1/sessoes/" + id + "/posicao", "{\"codigoPonto\":\"CEN-03\"}");

        assertEquals(200, resposta.status());
        assertEquals(id.toString(), resposta.texto("id"));
        assertEquals(List.of("CEN-03"), recentrar.codigos);
    }

    @Test
    @DisplayName("PUT posicao sem codigo devolve 400 apontando o campo")
    void recentrarSemCodigo() throws Exception {
        /*
         * Diferente de POST /sessoes, onde o codigo e opcional: aqui ele e o proprio objeto da
         * operacao, e uma requisicao sem ele nao quer dizer nada.
         */
        ServidorDeTeste.Resposta resposta = servidor.json("PUT",
                "/api/v1/sessoes/" + UUID.randomUUID() + "/posicao", "{}");

        assertEquals(400, resposta.status());
        assertEquals("codigoPonto", resposta.texto("validationErrors[0].field"));
        assertTrue(recentrar.codigos.isEmpty());
    }

    @Test
    @DisplayName("PUT posicao com placa desconhecida devolve 404")
    void recentrarPlacaDesconhecida() throws Exception {
        recentrar.falha = new RecursoNaoEncontradoException("Placa de localizacao", "ZZZ-99");

        ServidorDeTeste.Resposta resposta = servidor.json("PUT",
                "/api/v1/sessoes/" + UUID.randomUUID() + "/posicao", "{\"codigoPonto\":\"ZZZ-99\"}");

        assertEquals(404, resposta.status());
    }

    @Test
    @DisplayName("id malformado na URL vira 400, nao 500")
    void idMalformado() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/sessoes/nao-e-um-uuid");

        assertEquals(400, resposta.status());
        assertEquals(400, resposta.inteiro("status"));
        assertEquals("Parâmetro Inválido", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("nao-e-um-uuid"));
    }
}
