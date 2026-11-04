package br.com.jence.backend.dao;

import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.ChatMensagem;
import br.com.jence.backend.model.Remetente;
import br.com.jence.backend.model.Sessao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** {@link ChatMensagemDao} contra o Oracle, numa transacao que nunca e confirmada. */
@Tag("integracao")
class ChatMensagemDaoIntegracaoTest {

    private final ChatMensagemDao dao = new ChatMensagemDao();
    private Transacao transacao;

    @BeforeEach
    void abrir() throws Exception {
        transacao = Transacao.iniciar();
    }

    @AfterEach
    void desfazer() throws Exception {
        transacao.close();
    }

    @Test
    @DisplayName("historico volta em ordem cronologica, com texto longo inteiro")
    void historico() throws Exception {
        Sessao sessao = new SessaoDao().salvar(Sessao.iniciar(UUID.randomUUID()));
        String longa = "x".repeat(5000);

        ChatMensagem pergunta = dao.salvar(ChatMensagem.doCliente(UUID.randomUUID(), sessao.getId(), "Onde fica a tinta?"));
        Thread.sleep(5);
        ChatMensagem resposta = dao.salvar(ChatMensagem.doAssistente(UUID.randomUUID(), sessao.getId(), longa));

        List<ChatMensagem> historico = dao.buscarHistorico(sessao.getId());

        assertEquals(List.of(pergunta.getId(), resposta.getId()),
                historico.stream().map(ChatMensagem::getId).toList());
        assertEquals(Remetente.USER, historico.get(0).getRemetente());
        assertEquals(pergunta.getEnviadoEm(), historico.get(0).getEnviadoEm());
        assertEquals(longa, historico.get(1).getConteudo());
    }
}
