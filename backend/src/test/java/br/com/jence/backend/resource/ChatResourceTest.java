package br.com.jence.backend.resource;

import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.model.Remetente;
import br.com.jence.backend.service.ConsultarHistoricoChatUseCase;
import br.com.jence.backend.service.ConversarComAssistenteUseCase;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As rotas do chat pelo Jersey, com casos de uso falsos. */
class ChatResourceTest {

    /** Anota cada pergunta; devolve a resposta combinada ou lanca a falha combinada. */
    static class ConversarFalso extends ConversarComAssistenteUseCase {
        final List<String> perguntas = new ArrayList<>();
        ChatMensagemResponse resposta;
        Exception falha;

        ConversarFalso() {
            super(null, null, null, null);
        }

        @Override
        public ChatMensagemResponse executar(UUID sessaoId, String pergunta)
                throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
            perguntas.add(pergunta);
            if (falha instanceof RecursoNaoEncontradoException e) {
                throw e;
            }
            if (falha instanceof OperacaoNaoPermitidaException e) {
                throw e;
            }
            return resposta;
        }
    }

    static class HistoricoFalso extends ConsultarHistoricoChatUseCase {
        List<ChatMensagemResponse> mensagens = List.of();
        RecursoNaoEncontradoException falha;

        HistoricoFalso() {
            super(null, null);
        }

        @Override
        public List<ChatMensagemResponse> executar(UUID sessaoId) throws RecursoNaoEncontradoException {
            if (falha != null) {
                throw falha;
            }
            return mensagens;
        }
    }

    private final UUID sessaoId = UUID.randomUUID();
    private final ConversarFalso conversar = new ConversarFalso();
    private final HistoricoFalso historico = new HistoricoFalso();
    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(new ChatResource(conversar, historico));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    private ChatMensagemResponse mensagem(Remetente remetente, String conteudo) {
        ChatMensagemResponse mensagem = new ChatMensagemResponse();
        mensagem.setId(UUID.randomUUID());
        mensagem.setSessaoId(sessaoId);
        mensagem.setRemetente(remetente);
        mensagem.setConteudo(conteudo);
        mensagem.setEnviadoEm(LocalDateTime.now());
        return mensagem;
    }

    private String mensagens() {
        return "/api/v1/sessoes/" + sessaoId + "/chat/mensagens";
    }

    private ServidorDeTeste.Resposta enviar(String corpo) throws Exception {
        return servidor.json("POST", mensagens(), corpo);
    }

    // ---------------------------------------------------------------- envio

    @Test
    @DisplayName("POST devolve 201 com a resposta do assistente, nao com a pergunta")
    void enviarMensagem() throws Exception {
        conversar.resposta = mensagem(Remetente.ASSISTANT,
                "Voce vai precisar de tinta, rolo e lixa. Tudo no corredor Tintas.");

        ServidorDeTeste.Resposta resposta = enviar("""
                {"conteudo": "o que preciso para pintar uma parede?"}""");

        assertEquals(201, resposta.status());
        assertEquals("ASSISTANT", resposta.texto("remetente"));
        assertEquals(sessaoId.toString(), resposta.texto("sessaoId"));
        assertTrue(resposta.texto("conteudo").contains("corredor Tintas"));
        assertNotNull(resposta.json("enviadoEm"));
    }

    @Test
    @DisplayName("o conteudo enviado chega intacto ao caso de uso")
    void repassaOConteudo() throws Exception {
        conversar.resposta = mensagem(Remetente.ASSISTANT, "resposta");

        assertEquals(201, enviar("""
                {"conteudo": "quanto custa a torneira cromada?"}""").status());

        assertEquals(List.of("quanto custa a torneira cromada?"), conversar.perguntas);
    }

    @Test
    @DisplayName("conteudo em branco devolve 400 sem gastar chamada de IA")
    void conteudoEmBranco() throws Exception {
        ServidorDeTeste.Resposta resposta = enviar("""
                {"conteudo": "   "}""");

        assertEquals(400, resposta.status());
        assertEquals("conteudo", resposta.texto("validationErrors[0].field"));
        assertTrue(conversar.perguntas.isEmpty());
    }

    @Test
    @DisplayName("conteudo acima do limite devolve 400 sem gastar chamada de IA")
    void conteudoLongoDemais() throws Exception {
        ServidorDeTeste.Resposta resposta = enviar("{\"conteudo\": \"" + "a".repeat(1001) + "\"}");

        assertEquals(400, resposta.status());
        assertEquals("conteudo", resposta.texto("validationErrors[0].field"));
        assertTrue(conversar.perguntas.isEmpty());
    }

    @Test
    @DisplayName("sessao encerrada devolve 409")
    void sessaoInativa() throws Exception {
        conversar.falha = new OperacaoNaoPermitidaException("Sessao nao esta mais ativa (status COMPLETED)");

        ServidorDeTeste.Resposta resposta = enviar("""
                {"conteudo": "oi"}""");

        assertEquals(409, resposta.status());
        assertEquals("Operação Não Permitida", resposta.texto("error"));
    }

    @Test
    @DisplayName("sessao inexistente devolve 404")
    void sessaoInexistente() throws Exception {
        conversar.falha = new RecursoNaoEncontradoException("Sessao", sessaoId);

        assertEquals(404, enviar("""
                {"conteudo": "oi"}""").status());
    }

    @Test
    @DisplayName("corpo ilegivel devolve 400, nao 500")
    void corpoIlegivel() throws Exception {
        assertEquals(400, enviar("{isso nao e json").status());
    }

    // ---------------------------------------------------------------- historico

    @Test
    @DisplayName("GET devolve a conversa na ordem em que aconteceu")
    void historicoEmOrdem() throws Exception {
        historico.mensagens = List.of(
                mensagem(Remetente.USER, "quero reformar o banheiro"),
                mensagem(Remetente.ASSISTANT, "comece pela torneira e pelo sifao"),
                mensagem(Remetente.USER, "e a parte hidraulica?"),
                mensagem(Remetente.ASSISTANT, "cano PVC e cola, no corredor Encanamento"));

        ServidorDeTeste.Resposta resposta = servidor.get(mensagens());

        assertEquals(200, resposta.status());
        assertEquals(4, resposta.tamanho(""));
        assertEquals("USER", resposta.texto("[0].remetente"));
        assertEquals("quero reformar o banheiro", resposta.texto("[0].conteudo"));
        assertEquals("ASSISTANT", resposta.texto("[1].remetente"));
        assertTrue(resposta.texto("[3].conteudo").contains("Encanamento"));
    }

    @Test
    @DisplayName("conversa que ainda nao comecou devolve 200 com lista vazia")
    void historicoVazio() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.get(mensagens());

        assertEquals(200, resposta.status());
        assertEquals(0, resposta.tamanho(""));
    }

    @Test
    @DisplayName("sessao inexistente devolve 404, distinguivel de conversa vazia")
    void historicoDeSessaoInexistente() throws Exception {
        historico.falha = new RecursoNaoEncontradoException("Sessao", sessaoId);

        ServidorDeTeste.Resposta resposta = servidor.get(mensagens());

        assertEquals(404, resposta.status());
        assertEquals("Recurso Não Encontrado", resposta.texto("error"));
    }

    @Test
    @DisplayName("sessaoId malformado devolve 400, nao 500")
    void sessaoIdMalformado() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.get("/api/v1/sessoes/nao-e-uuid/chat/mensagens");

        assertEquals(400, resposta.status());
        assertEquals("Parâmetro Inválido", resposta.texto("error"));
    }
}
