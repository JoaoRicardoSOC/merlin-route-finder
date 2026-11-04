package br.com.jence.backend;

import br.com.jence.backend.dao.ChatMensagemDao;
import br.com.jence.backend.dto.ChatMensagemResponse;
import br.com.jence.backend.service.ConversarComAssistenteUseCase;
import br.com.jence.backend.service.InicializarSessaoUseCase;
import br.com.jence.backend.model.ChatMensagem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Conversa real com o Gemini sobre o catalogo real. Exige Oracle e GEMINI_API_KEY. */
@Tag("integracao")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class AssistenteIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired ConversarComAssistenteUseCase conversar;
    private final ChatMensagemDao chatMensagemDao = BancoDeTeste.mensagens();

    private UUID sessaoId;

    /*
     * O tier gratuito tem limite por minuto alem do limite diario. Um respiro entre os testes
     * evita que eles disputem cota entre si e midam a cota em vez do comportamento.
     */
    @BeforeEach
    void criarSessao() throws Exception {
        Thread.sleep(5_000);
        sessaoId = inicializar.executar(null).id();
    }

    @AfterEach
    void limpar() throws Exception {
        BancoDeTeste.apagarSessao(sessaoId);
    }

    @Test
    void sugereProdutosReaisDoCatalogo() throws Exception {
        ChatMensagemResponse r = conversar.executar(sessaoId, "O que eu preciso para pintar uma parede?");

        System.out.println(">>> pergunta: O que eu preciso para pintar uma parede?");
        System.out.println(">>> resposta: " + r.conteudo());

        assertFalse(r.conteudo() == null || r.conteudo().isBlank());
        // A massa tem tinta, rolo, lixa e fita crepe em Tintas: a resposta deve sair dali.
        String conteudo = r.conteudo().toLowerCase();
        assertTrue(List.of("tinta", "rolo", "lixa").stream().anyMatch(conteudo::contains),
                "a sugestao precisa citar produtos que existem no catalogo");
    }

    @Test
    void recusaAssuntoForaDoEscopo() throws Exception {
        ChatMensagemResponse r = conversar.executar(sessaoId,
                "Quem foi o vencedor da Copa do Mundo de 2022 e por que?");

        System.out.println(">>> pergunta fora de escopo -> " + r.conteudo());

        String conteudo = r.conteudo().toLowerCase();
        assertFalse(conteudo.contains("argentina"),
                "nao pode responder ao merito do assunto fora de escopo");
        assertFalse(conteudo.contains("messi"),
                "nao pode responder ao merito do assunto fora de escopo");
    }

    @Test
    void mantemAConversaEmOrdemCronologica() throws Exception {
        conversar.executar(sessaoId, "Quero reformar o banheiro. Por onde começo?");
        Thread.sleep(5_000);
        conversar.executar(sessaoId, "E para a parte hidraulica?");

        List<ChatMensagem> historico = chatMensagemDao.buscarHistorico(sessaoId);

        System.out.println(">>> historico com " + historico.size() + " mensagens:");
        historico.forEach(m -> System.out.printf("    [%s] %s%n",
                m.getRemetente(),
                m.getConteudo().length() > 90 ? m.getConteudo().substring(0, 90) + "..." : m.getConteudo()));

        assertEquals(4, historico.size());
        assertTrue(historico.get(0).isDoCliente());
        assertFalse(historico.get(1).isDoCliente());
        assertEquals("E para a parte hidraulica?", historico.get(2).getConteudo());
        assertFalse(historico.get(3).isDoCliente());
    }
}
