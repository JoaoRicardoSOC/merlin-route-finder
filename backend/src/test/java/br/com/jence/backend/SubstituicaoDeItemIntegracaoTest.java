package br.com.jence.backend;

import br.com.jence.backend.application.dto.ListaRoteiroResponse;
import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.ConcluirRotaUseCase;
import br.com.jence.backend.application.usecase.ConsultarSessaoUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.SubstituirItemDoRoteiroUseCase;
import br.com.jence.backend.domain.entity.OrigemSugestao;
import br.com.jence.backend.domain.entity.RegistroRuptura;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.domain.repository.RegistroRupturaRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.RegistroRupturaJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A troca do produto que faltou pelo substituto, que e o que fecha o ciclo da ruptura.
 * <p>
 * A promessa do recurso e converter prateleira vazia em venda. Se aceitar a sugestao der
 * trabalho - duas acoes em pe no corredor -, a conversao nao acontece e o recurso vira
 * decoracao.
 * <p>
 * Exige banco, nao exige GEMINI_API_KEY.
 */
@Tag("integracao")
@SpringBootTest
class SubstituicaoDeItemIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired MarcarItemColetadoUseCase marcarColetado;
    @Autowired SubstituirItemDoRoteiroUseCase substituir;
    @Autowired ConsultarSessaoUseCase consultar;
    @Autowired ConcluirRotaUseCase concluir;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired RegistroRupturaRepository registroRupturaRepository;

    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;
    @Autowired RegistroRupturaJpaRepository rupturaJpa;

    /** A lixa grao 120 e o item que some da prateleira; a lixa d'agua e o substituto. */
    private static final String EM_FALTA = "SKU-TIN-003";
    private static final String SUBSTITUTO = "SKU-TIN-004";

    private final List<UUID> criadas = new ArrayList<>();

    @AfterEach
    void limpar() {
        criadas.forEach(id -> {
            /*
             * O registro de ruptura aponta para a sessao por chave estrangeira, entao apagar a
             * sessao antes dele e recusado com ORA-02292. E o mesmo motivo pelo qual a limpeza
             * de outros testes de integracao falha em silencio quando uma ruptura acontece
             * neles - ver O-20.
             */
            rupturaJpa.deleteAll(rupturaJpa.findBySessaoIdOrderByRegistradoEmDesc(id));
            listaJpa.findBySessaoId(id).ifPresent(listaJpa::delete);
            sessaoJpa.deleteById(id);
        });
        criadas.clear();
    }

    private UUID novaSessao() {
        SessaoResponse sessao = inicializar.executar("ENT-01");
        criadas.add(sessao.id());
        return sessao.id();
    }

    private UUID idDe(String sku) {
        return produtoRepository.buscarPorSku(sku).orElseThrow().getId();
    }

    private UUID adicionar(UUID sessaoId, String sku) {
        return adicionar.executar(sessaoId, idDe(sku)).id();
    }

    private List<String> skusDaLista(ListaRoteiroResponse lista) {
        return lista.itens().stream().map(item -> item.produto().sku()).sorted().toList();
    }

    // ---------------------------------------------------------------- a troca

    @Test
    @DisplayName("numa chamada so, o substituto entra e o item em falta sai")
    void trocaNumaChamada() {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        adicionar(sessaoId, "SKU-ENC-001");

        ListaRoteiroResponse depois = substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertThat(skusDaLista(depois)).containsExactly("SKU-ENC-001", SUBSTITUTO);
        assertThat(depois.quantidadeItens())
                .as("trocar nao pode mudar o tamanho da lista")
                .isEqualTo(2);
    }

    @Test
    @DisplayName("o substituto entra NAO coletado, mesmo trocando um item ja coletado")
    void substitutoEntraNaoColetado() {
        /*
         * O detalhe que decide se o cliente sai da loja com o produto. O substituto pode estar
         * alguns metros adiante, e herdar o estado do item que saiu o marcaria como pego -
         * fazendo o cliente ir embora sem ele, e mentindo sobre onde ele esta.
         */
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        marcarColetado.executar(lixaEmFalta);

        ListaRoteiroResponse depois = substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertThat(depois.itens()).singleElement()
                .satisfies(item -> {
                    assertThat(item.produto().sku()).isEqualTo(SUBSTITUTO);
                    assertThat(item.coletado()).isFalse();
                });
    }

    @Test
    @DisplayName("o substituto que ja estava na lista nao vira duplicata")
    void semDuplicata() {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        adicionar(sessaoId, SUBSTITUTO);

        ListaRoteiroResponse depois = substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertThat(skusDaLista(depois))
                .as("o cliente ja tinha a lixa d'agua na lista; ela nao pode aparecer duas vezes")
                .containsExactly(SUBSTITUTO);
    }

    @Test
    @DisplayName("a posicao do cliente nao pula para a prateleira do substituto")
    void posicaoNaoPula() {
        // Ele ainda nao foi buscar o substituto: a posicao continua sendo a ultima evidencia
        // real, e nao um palpite sobre onde ele vai estar.
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertThat(consultar.executar(sessaoId).posicaoAtual().codigoCurto()).isEqualTo("ENT01");
    }

    // ---------------------------------------------------------------- o que a troca nao apaga

    @Test
    @DisplayName("a ruptura continua registrada depois da troca")
    void rupturaPermanece() {
        /*
         * O registro e evidencia do que aconteceu na gondola, e vale tenha o cliente aceitado
         * a troca ou nao. Apaga-lo faria a loja perder exatamente o dado que o recurso existe
         * para produzir - e comparar quantas rupturas viraram troca e o que diz se as
         * sugestoes estao boas.
         *
         * O registro e plantado direto pelo repositorio, e nao pelo caso de uso da ruptura,
         * para o teste nao depender do assistente de IA: a cota gratuita torna a suite
         * instavel (O-01), e o que se verifica aqui e a troca, nao a sugestao.
         */
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        registroRupturaRepository.salvar(RegistroRuptura.comSugestao(
                sessaoId, lixaEmFalta, idDe(EM_FALTA), idDe(SUBSTITUTO),
                "A lixa d'agua grao 150 da o mesmo acabamento.", OrigemSugestao.ASSISTENTE_IA));

        substituir.executar(lixaEmFalta, idDe(SUBSTITUTO));

        assertThat(registroRupturaRepository.buscarPorSessao(sessaoId))
                .as("trocar o item nao pode apagar o relato da prateleira vazia")
                .singleElement()
                .satisfies(registro -> {
                    assertThat(registro.getProdutoFaltanteId()).isEqualTo(idDe(EM_FALTA));
                    assertThat(registro.getItemRoteiroId())
                            .as("o registro aponta para um item que ja nao existe, e tudo bem: "
                                    + "ele descreve o que aconteceu, nao o estado de agora")
                            .isEqualTo(lixaEmFalta);
                });
    }

    // ---------------------------------------------------------------- recusas

    @Test
    @DisplayName("trocar um produto por ele mesmo e recusado, e nao apaga o item")
    void naoSubstituiPorSiMesmo() {
        /*
         * Sem a guarda isto apagaria o item em silencio: o adicionar devolveria o item
         * existente e o remover o apagaria em seguida. O cliente ficaria sem o produto sem ter
         * pedido isso.
         */
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        assertThatThrownBy(() -> substituir.executar(lixaEmFalta, idDe(EM_FALTA)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);

        assertThat(listaJpa.findBySessaoId(sessaoId).orElseThrow().getItens()).hasSize(1);
    }

    @Test
    @DisplayName("produto substituto inexistente devolve 404 e a lista fica intacta")
    void substitutoInexistente() {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);

        assertThatThrownBy(() -> substituir.executar(lixaEmFalta, UUID.randomUUID()))
                .isInstanceOf(RecursoNaoEncontradoException.class);

        assertThat(listaJpa.findBySessaoId(sessaoId).orElseThrow().getItens()).hasSize(1);
    }

    @Test
    @DisplayName("sessao encerrada recusa a troca")
    void sessaoEncerrada() {
        UUID sessaoId = novaSessao();
        UUID lixaEmFalta = adicionar(sessaoId, EM_FALTA);
        concluir.executar(sessaoId);

        assertThatThrownBy(() -> substituir.executar(lixaEmFalta, idDe(SUBSTITUTO)))
                .isInstanceOf(OperacaoNaoPermitidaException.class);
    }

    @Test
    @DisplayName("item inexistente devolve 404")
    void itemInexistente() {
        assertThatThrownBy(() -> substituir.executar(UUID.randomUUID(), idDe(SUBSTITUTO)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }
}
