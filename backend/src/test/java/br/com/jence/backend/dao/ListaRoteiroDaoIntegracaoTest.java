package br.com.jence.backend.dao;

import br.com.jence.backend.factory.ConnectionFactory;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.Sessao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link ListaRoteiroDao} e {@link ItemRoteiroDao} contra o Oracle.
 * <p>
 * Diferente dos outros testes de DAO, este confirma o que grava: o cenario da gravacao atrasada
 * precisa de duas conexoes, e a segunda so enxerga o que a primeira confirmou. Por isso cada
 * teste cria a propria sessao e apaga tudo no fim.
 */
@Tag("integracao")
class ListaRoteiroDaoIntegracaoTest {

    private final ListaRoteiroDao listas = new ListaRoteiroDao();
    private final ItemRoteiroDao itens = new ItemRoteiroDao();

    private UUID sessaoId;
    private UUID listaId;
    private UUID primeiroItem;
    private Produto trena;
    private Produto outro;

    @BeforeEach
    void montar() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            ProdutoDao produtos = new ProdutoDao();
            trena = produtos.buscarPorSku("SKU-FER-002").orElseThrow();
            outro = produtos.buscarPaginado(0, 2).conteudo().stream()
                    .filter(p -> !p.getId().equals(trena.getId())).findFirst().orElseThrow();

            sessaoId = new SessaoDao().salvar(Sessao.iniciar(UUID.randomUUID())).getId();
            ListaRoteiro lista = ListaRoteiro.criarPara(UUID.randomUUID(), sessaoId);
            primeiroItem = lista.adicionarProduto(trena).getId();
            listas.salvar(lista);
            listaId = lista.getId();
            transacao.confirmar();
        }
    }

    @AfterEach
    void limpar() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            for (String sql : new String[]{
                    "delete from tb_ch26_item_roteiro where lista_id = ?",
                    "delete from tb_ch26_lista_roteiro where id = ?"}) {
                try (PreparedStatement stm = ConnectionFactory.getConnection().prepareStatement(sql)) {
                    stm.setString(1, listaId.toString());
                    stm.executeUpdate();
                }
            }
            try (PreparedStatement stm = ConnectionFactory.getConnection()
                    .prepareStatement("delete from tb_ch26_sessao where id = ?")) {
                stm.setString(1, sessaoId.toString());
                stm.executeUpdate();
            }
            transacao.confirmar();
        }
    }

    private ListaRoteiro lerDeNovo() throws Exception {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            return listas.buscarPorId(listaId).orElseThrow();
        }
    }

    private static Set<UUID> ids(ListaRoteiro lista) {
        return lista.getItens().stream().map(ItemRoteiro::getId).collect(Collectors.toSet());
    }

    @Test
    @DisplayName("as tres buscas leem a mesma lista, com o produto e o ponto de cada item")
    void buscas() throws Exception {
        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            ListaRoteiro porId = listas.buscarPorId(listaId).orElseThrow();
            ListaRoteiro porSessao = listas.buscarPorSessao(sessaoId).orElseThrow();
            ListaRoteiro porItem = listas.buscarPorItem(primeiroItem).orElseThrow();

            assertEquals(Set.of(primeiroItem), ids(porId));
            assertEquals(listaId, porSessao.getId());
            assertEquals(listaId, porItem.getId());
            assertEquals(trena.getNome(), porId.getItens().getFirst().getProduto().getNome());
            assertNotNull(porId.getItens().getFirst().getProduto().getPontoMapa());
            assertTrue(listas.buscarPorItem(UUID.randomUUID()).isEmpty());
        }
    }

    @Test
    @DisplayName("adicionar, marcar e remover pela lista gravam o que mudou")
    void alteracoesPelaLista() throws Exception {
        UUID novo;
        try (Transacao transacao = Transacao.iniciar()) {
            ListaRoteiro lista = listas.buscarPorId(listaId).orElseThrow();
            novo = lista.adicionarProduto(outro).getId();
            lista.getItens().stream().filter(i -> i.getId().equals(primeiroItem)).findFirst()
                    .orElseThrow().marcarComoColetado(LocalDateTime.now());
            listas.salvar(lista);
            transacao.confirmar();
        }

        ListaRoteiro depois = lerDeNovo();
        assertEquals(Set.of(primeiroItem, novo), ids(depois));
        for (ItemRoteiro item : depois.getItens()) {
            assertEquals(item.getId().equals(primeiroItem), item.isColetado());
        }

        try (Transacao transacao = Transacao.iniciar()) {
            ListaRoteiro lista = listas.buscarPorId(listaId).orElseThrow();
            assertTrue(lista.removerProduto(novo));
            listas.salvar(lista);
            transacao.confirmar();
        }

        assertEquals(Set.of(primeiroItem), ids(lerDeNovo()));
    }

    @Test
    @DisplayName("gravacao atrasada nao apaga item que outra transacao incluiu depois da leitura (D-48)")
    void gravacaoAtrasadaNaoApagaItemNovo() throws Exception {
        UUID incluidoPorOutra;

        try (Transacao lenta = Transacao.iniciar()) {
            ListaRoteiro desatualizada = listas.buscarPorId(listaId).orElseThrow();

            try (Transacao rapida = Transacao.iniciarNova()) {
                ListaRoteiro lista = listas.buscarPorId(listaId).orElseThrow();
                incluidoPorOutra = lista.adicionarProduto(outro).getId();
                listas.salvar(lista);
                rapida.confirmar();
            }

            desatualizada.getItens().getFirst().marcarComoColetado(LocalDateTime.now());
            listas.salvar(desatualizada);
            lenta.confirmar();
        }

        ListaRoteiro depois = lerDeNovo();
        assertEquals(Set.of(primeiroItem, incluidoPorOutra), ids(depois));
        for (ItemRoteiro item : depois.getItens()) {
            assertEquals(item.getId().equals(primeiroItem), item.isColetado());
        }
    }

    @Test
    @DisplayName("o ItemRoteiroDao le o item e grava a coleta, e recusa item que nao existe")
    void itemIsolado() throws Exception {
        try (Transacao transacao = Transacao.iniciar()) {
            ItemRoteiro item = itens.buscarPorId(primeiroItem).orElseThrow();
            assertFalse(item.isColetado());

            item.marcarComoColetado(LocalDateTime.now());
            itens.salvar(item);
            transacao.confirmar();
        }

        try (Transacao transacao = Transacao.iniciarSomenteLeitura()) {
            ItemRoteiro item = itens.buscarPorId(primeiroItem).orElseThrow();
            assertTrue(item.isColetado());
            assertNull(itens.buscarPorId(UUID.randomUUID()).orElse(null));
            assertThrows(IllegalArgumentException.class,
                    () -> itens.salvar(new ItemRoteiro(UUID.randomUUID(), trena)));
        }
    }
}
