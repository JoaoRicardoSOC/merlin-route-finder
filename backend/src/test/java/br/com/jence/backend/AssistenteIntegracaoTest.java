package br.com.jence.backend;

import br.com.jence.backend.application.dto.ChatMensagemResponse;
import br.com.jence.backend.application.usecase.ConversarComAssistenteUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.domain.repository.ChatMensagemRepository;
import br.com.jence.backend.infrastructure.database.repository.ChatMensagemJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** Conversa real com o Gemini sobre o catalogo real. Exige Oracle e GEMINI_API_KEY. */
@Tag("integracao")
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class AssistenteIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired ConversarComAssistenteUseCase conversar;
    @Autowired ChatMensagemRepository chatRepository;
    @Autowired ChatMensagemJpaRepository chatJpa;
    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;

    private UUID sessaoId;

    /*
     * O tier gratuito tem limite por minuto alem do limite diario. Um respiro entre os testes
     * evita que eles disputem cota entre si e midam a cota em vez do comportamento.
     */
    @BeforeEach
    void criarSessao() throws InterruptedException {
        Thread.sleep(5_000);
        sessaoId = inicializar.executar(null).id();
    }

    @AfterEach
    void limpar() {
        chatJpa.findBySessaoIdOrderByEnviadoEmAsc(sessaoId).forEach(chatJpa::delete);
        listaJpa.findBySessaoId(sessaoId).ifPresent(listaJpa::delete);
        sessaoJpa.deleteById(sessaoId);
    }

    @Test
    void sugereProdutosReaisDoCatalogo() {
        ChatMensagemResponse r = conversar.executar(sessaoId, "O que eu preciso para pintar uma parede?");

        System.out.println(">>> pergunta: O que eu preciso para pintar uma parede?");
        System.out.println(">>> resposta: " + r.conteudo());

        assertThat(r.conteudo()).isNotBlank();
        // A massa tem tinta, rolo, lixa e fita crepe em Tintas: a resposta deve sair dali.
        assertThat(r.conteudo().toLowerCase())
                .as("a sugestao precisa citar produtos que existem no catalogo")
                .containsAnyOf("tinta", "rolo", "lixa");
    }

    @Test
    void recusaAssuntoForaDoEscopo() {
        ChatMensagemResponse r = conversar.executar(sessaoId,
                "Quem foi o vencedor da Copa do Mundo de 2022 e por que?");

        System.out.println(">>> pergunta fora de escopo -> " + r.conteudo());

        assertThat(r.conteudo().toLowerCase())
                .as("nao pode responder ao merito do assunto fora de escopo")
                .doesNotContain("argentina")
                .doesNotContain("messi");
    }

    @Test
    void mantemAConversaEmOrdemCronologica() throws InterruptedException {
        conversar.executar(sessaoId, "Quero reformar o banheiro. Por onde começo?");
        Thread.sleep(5_000);
        conversar.executar(sessaoId, "E para a parte hidraulica?");

        List<ChatMensagem> historico = chatRepository.buscarHistorico(sessaoId);

        System.out.println(">>> historico com " + historico.size() + " mensagens:");
        historico.forEach(m -> System.out.printf("    [%s] %s%n",
                m.getRemetente(),
                m.getConteudo().length() > 90 ? m.getConteudo().substring(0, 90) + "..." : m.getConteudo()));

        assertThat(historico).hasSize(4);
        assertThat(historico.get(0).isDoCliente()).isTrue();
        assertThat(historico.get(1).isDoCliente()).isFalse();
        assertThat(historico.get(2).getConteudo()).isEqualTo("E para a parte hidraulica?");
        assertThat(historico.get(3).isDoCliente()).isFalse();
    }
}
