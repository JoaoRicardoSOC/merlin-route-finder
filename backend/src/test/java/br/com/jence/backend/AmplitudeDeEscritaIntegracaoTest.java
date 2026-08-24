package br.com.jence.backend;

import br.com.jence.backend.application.usecase.AdicionarProdutoAoRoteiroUseCase;
import br.com.jence.backend.application.usecase.InicializarSessaoUseCase;
import br.com.jence.backend.application.usecase.MarcarItemColetadoUseCase;
import br.com.jence.backend.application.usecase.RemoverProdutoDoRoteiroUseCase;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import br.com.jence.backend.infrastructure.database.repository.ListaRoteiroJpaRepository;
import br.com.jence.backend.infrastructure.database.repository.SessaoJpaRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Quantas linhas cada operacao do roteiro escreve no banco.
 * <p>
 * Marcar um item como coletado toca <b>uma</b> linha; adicionar insere uma; remover apaga uma.
 * Nenhuma delas reescreve os irmaos, apesar de os casos de uso gravarem pelo agregado - porque
 * o Hibernate compara com o estado de quando carregou e so escreve o que mudou (D-48).
 * <p>
 * O teste mais util aqui e o do roteiro de oito itens: ele fixa que <b>o custo de marcar um
 * item nao cresce com o tamanho da lista</b>, o que importa para o cliente B2B que monta a
 * obra inteira num carrinho sem limite (D-17).
 * <p>
 * Serve tambem de alarme: se alguem um dia trocar a gravacao por um caminho que remonte o
 * agregado fora da transacao de leitura, estes numeros sobem e o teste avisa.
 */
@Tag("integracao")
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class AmplitudeDeEscritaIntegracaoTest {

    @Autowired InicializarSessaoUseCase inicializar;
    @Autowired AdicionarProdutoAoRoteiroUseCase adicionar;
    @Autowired MarcarItemColetadoUseCase marcarColetado;
    @Autowired RemoverProdutoDoRoteiroUseCase remover;
    @Autowired ListaRoteiroRepository listaRoteiroRepository;
    @Autowired ProdutoRepository produtoRepository;
    @Autowired EntityManagerFactory entityManagerFactory;

    @Autowired ListaRoteiroJpaRepository listaJpa;
    @Autowired SessaoJpaRepository sessaoJpa;

    private UUID sessaoId;
    private Statistics estatisticas;

    @BeforeEach
    void montarRoteiro() {
        sessaoId = inicializar.executar().id();
        adicionarAoRoteiro("SKU-TIN-001");
        adicionarAoRoteiro("SKU-ENC-001");
        adicionarAoRoteiro("SKU-JAR-001");

        estatisticas = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    @AfterEach
    void limpar() {
        listaJpa.findBySessaoId(sessaoId).ifPresent(listaJpa::delete);
        sessaoJpa.deleteById(sessaoId);
    }

    private void adicionarAoRoteiro(String sku) {
        adicionar.executar(sessaoId, produtoRepository.buscarPorSku(sku).orElseThrow().getId());
    }

    private List<ItemRoteiro> itens() {
        return listaRoteiroRepository.buscarPorSessao(sessaoId).orElseThrow().getItens().stream()
                .sorted(Comparator.comparing(i -> i.getProduto().getSku()))
                .toList();
    }

    /** Linhas de item gravadas, inseridas ou apagadas por uma operacao. */
    private record Escrita(long atualizadas, long inseridas, long apagadas) {

        long total() {
            return atualizadas + inseridas + apagadas;
        }

        @Override
        public String toString() {
            return "%d update(s), %d insert(s), %d delete(s)".formatted(atualizadas, inseridas, apagadas);
        }
    }

    private Escrita medir(Runnable operacao) {
        String item = "br.com.jence.backend.infrastructure.database.entity.ItemRoteiroEntity";
        var antes = estatisticas.getEntityStatistics(item);
        long u = antes.getUpdateCount();
        long i = antes.getInsertCount();
        long d = antes.getDeleteCount();

        operacao.run();

        var depois = estatisticas.getEntityStatistics(item);
        return new Escrita(depois.getUpdateCount() - u,
                depois.getInsertCount() - i,
                depois.getDeleteCount() - d);
    }

    @Test
    @DisplayName("marcar um item toca a linha daquele item, e so ela")
    void marcarTocaUmaLinha() {
        UUID alvo = itens().getFirst().getId();

        Escrita escrita = medir(() -> marcarColetado.executar(alvo));

        System.out.println(">>> marcar item coletado (roteiro de 3): " + escrita);

        assertThat(escrita.atualizadas())
                .as("so a linha do item marcado deveria mudar")
                .isEqualTo(1);
        assertThat(escrita.apagadas())
                .as("marcar como coletado nunca deveria apagar item nenhum")
                .isZero();
    }

    @Test
    @DisplayName("adicionar um produto insere uma linha e nao reescreve as existentes")
    void adicionarInsereUmaLinha() {
        UUID furadeira = produtoRepository.buscarPorSku("SKU-FER-001").orElseThrow().getId();

        Escrita escrita = medir(() -> adicionar.executar(sessaoId, furadeira));

        System.out.println(">>> adicionar produto (roteiro de 3): " + escrita);

        assertThat(escrita.inseridas()).isEqualTo(1);
        assertThat(escrita.atualizadas())
                .as("os itens que ja estavam la nao mudaram")
                .isZero();
        assertThat(escrita.apagadas()).isZero();
    }

    @Test
    @DisplayName("remover um item apaga uma linha e nao reescreve as outras")
    void removerApagaUmaLinha() {
        UUID alvo = itens().getLast().getId();

        Escrita escrita = medir(() -> remover.executar(sessaoId, alvo));

        System.out.println(">>> remover item (roteiro de 3): " + escrita);

        assertThat(escrita.apagadas()).isEqualTo(1);
        assertThat(escrita.atualizadas())
                .as("remover um item nao pode reescrever o estado dos outros")
                .isZero();
    }

    @Test
    @DisplayName("a amplitude nao cresce com o tamanho do roteiro")
    void amplitudeNaoCresceComORoteiro() {
        /*
         * Oito itens e o tamanho de uma lista de obra pequena. Se o custo de marcar um item
         * dependesse do tamanho do roteiro, o cliente B2B - que a D-17 deixou sem limite -
         * pagaria cada vez mais caro conforme a lista crescesse. Nao paga: continua sendo uma
         * linha, porque o Hibernate so escreve o que mudou.
         */
        for (String sku : List.of("SKU-FER-001", "SKU-ILU-001", "SKU-MAT-001",
                "SKU-COZ-001", "SKU-DEC-001")) {
            adicionarAoRoteiro(sku);
        }

        assertThat(itens()).hasSize(8);
        UUID alvo = itens().getFirst().getId();

        Escrita escrita = medir(() -> marcarColetado.executar(alvo));

        System.out.println(">>> marcar item coletado (roteiro de 8): " + escrita);

        assertThat(escrita.total())
                .as("o custo de marcar um item nao pode depender de quantos itens o cliente tem")
                .isEqualTo(1);
    }
}
