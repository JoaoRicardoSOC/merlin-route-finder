package br.com.jence.backend.resource;

import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.dto.ListaRoteiroResponse;
import br.com.jence.backend.dto.RupturaEstoqueResponse;
import br.com.jence.backend.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.exception.SubstitutoIndisponivelException;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.OrigemSugestao;
import br.com.jence.backend.model.PontoMapa;
import br.com.jence.backend.model.Produto;
import br.com.jence.backend.model.TipoPonto;
import br.com.jence.backend.service.DesmarcarItemColetadoUseCase;
import br.com.jence.backend.service.MarcarItemColetadoUseCase;
import br.com.jence.backend.service.SubstituirItemDoRoteiroUseCase;
import br.com.jence.backend.service.TratarRupturaEstoqueUseCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** As acoes sobre um item do roteiro pelo Jersey, com casos de uso falsos. */
class ItemRoteiroResourceTest {

    /** Lanca a falha configurada no falso da coleta, se houver. */
    static void lancar(Exception falha) throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
        if (falha instanceof RecursoNaoEncontradoException e) {
            throw e;
        }
        if (falha instanceof OperacaoNaoPermitidaException e) {
            throw e;
        }
    }

    static class MarcarFalso extends MarcarItemColetadoUseCase {
        ItemRoteiroDetalhadoResponse item;
        Exception falha;

        MarcarFalso() {
            super(null, null);
        }

        @Override
        public ItemRoteiroDetalhadoResponse executar(UUID itemId)
                throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
            lancar(falha);
            return item;
        }
    }

    static class DesmarcarFalso extends DesmarcarItemColetadoUseCase {
        final List<UUID> chamadas = new ArrayList<>();
        ItemRoteiroDetalhadoResponse item;
        Exception falha;

        DesmarcarFalso() {
            super(null, null);
        }

        @Override
        public ItemRoteiroDetalhadoResponse executar(UUID itemId)
                throws RecursoNaoEncontradoException, OperacaoNaoPermitidaException {
            chamadas.add(itemId);
            lancar(falha);
            return item;
        }
    }

    static class SubstituirFalso extends SubstituirItemDoRoteiroUseCase {
        final List<UUID[]> chamadas = new ArrayList<>();
        ListaRoteiroResponse lista;
        OperacaoNaoPermitidaException falha;

        SubstituirFalso() {
            super(null, null, null);
        }

        @Override
        public ListaRoteiroResponse executar(UUID itemId, UUID produtoSubstitutoId)
                throws OperacaoNaoPermitidaException {
            chamadas.add(new UUID[] {itemId, produtoSubstitutoId});
            if (falha != null) {
                throw falha;
            }
            return lista;
        }
    }

    static class RupturaFalsa extends TratarRupturaEstoqueUseCase {
        RupturaEstoqueResponse resposta;
        Exception falha;

        RupturaFalsa() {
            super(null, null, null, null, null, null);
        }

        @Override
        public RupturaEstoqueResponse executar(UUID itemId) throws RecursoNaoEncontradoException,
                OperacaoNaoPermitidaException, SubstitutoIndisponivelException {
            if (falha instanceof RecursoNaoEncontradoException e) {
                throw e;
            }
            if (falha instanceof OperacaoNaoPermitidaException e) {
                throw e;
            }
            if (falha instanceof SubstitutoIndisponivelException e) {
                throw e;
            }
            return resposta;
        }
    }

    private final MarcarFalso marcar = new MarcarFalso();
    private final DesmarcarFalso desmarcar = new DesmarcarFalso();
    private final RupturaFalsa ruptura = new RupturaFalsa();
    private final SubstituirFalso substituir = new SubstituirFalso();
    private ServidorDeTeste servidor;

    @BeforeEach
    void subir() throws Exception {
        servidor = ServidorDeTeste.com(new ItemRoteiroResource(marcar, desmarcar, ruptura, substituir));
    }

    @AfterEach
    void descer() {
        servidor.close();
    }

    private static Produto cano(UUID produtoId) {
        return new Produto(produtoId, "SKU-ENC-001", "Cano PVC Soldavel 25mm 6m",
                new BigDecimal("28.90"), 35,
                new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Encanamento", 50, 50));
    }

    private ListaRoteiroResponse lista(UUID sessaoId, ItemRoteiroDetalhadoResponse... itens) {
        ListaRoteiroResponse lista = new ListaRoteiroResponse();
        lista.setId(UUID.randomUUID());
        lista.setSessaoId(sessaoId);
        lista.setQuantidadeItens(itens.length);
        lista.setItens(List.of(itens));
        return lista;
    }

    private String item(Object itemId, String acao) {
        return "/api/v1/roteiro/itens/" + itemId + "/" + acao;
    }

    // ---------------------------------------------------------------- coleta (UC-014)

    @Test
    @DisplayName("PATCH coletar devolve o item marcado")
    void coletarItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        marcar.item = ItemRoteiroDetalhadoResponse.de(
                ItemRoteiro.reconstituir(itemId, cano(UUID.randomUUID()), LocalDateTime.now()));

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("PATCH", item(itemId, "coletar"));

        assertEquals(200, resposta.status());
        assertEquals(itemId.toString(), resposta.texto("id"));
        assertEquals(true, resposta.json("coletado"));
        assertEquals("Cano PVC Soldavel 25mm 6m", resposta.texto("produto.nome"));
    }

    @Test
    @DisplayName("PATCH desmarcar devolve o item sem coleta")
    void desmarcarItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        desmarcar.item = ItemRoteiroDetalhadoResponse.de(new ItemRoteiro(itemId, cano(UUID.randomUUID())));

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("PATCH", item(itemId, "desmarcar"));

        assertEquals(200, resposta.status());
        assertEquals(itemId.toString(), resposta.texto("id"));
        assertEquals(false, resposta.json("coletado"));
        assertEquals(List.of(itemId), desmarcar.chamadas);
    }

    @Test
    @DisplayName("desmarcar item inexistente devolve 404")
    void desmarcarItemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        desmarcar.falha = new RecursoNaoEncontradoException("Item do roteiro", itemId);

        assertEquals(404, servidor.semCorpo("PATCH", item(itemId, "desmarcar")).status());
    }

    @Test
    @DisplayName("coletar em sessao encerrada devolve 409")
    void coletarEmSessaoEncerrada() throws Exception {
        marcar.falha = new OperacaoNaoPermitidaException("Sessao nao esta mais ativa (status COMPLETED)");

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("PATCH", item(UUID.randomUUID(), "coletar"));

        assertEquals(409, resposta.status());
        assertEquals("Operação Não Permitida", resposta.texto("error"));
    }

    @Test
    @DisplayName("desmarcar em sessao encerrada devolve 409")
    void desmarcarEmSessaoEncerrada() throws Exception {
        desmarcar.falha = new OperacaoNaoPermitidaException("Sessao nao esta mais ativa (status COMPLETED)");

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("PATCH", item(UUID.randomUUID(), "desmarcar"));

        assertEquals(409, resposta.status());
        assertEquals("Operação Não Permitida", resposta.texto("error"));
    }

    // ---------------------------------------------------------------- aceitar o substituto

    @Test
    @DisplayName("POST substituir devolve a lista ja com a troca feita")
    void substituirItem() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID substitutoId = UUID.randomUUID();
        substituir.lista = lista(UUID.randomUUID(), ItemRoteiroDetalhadoResponse.de(new ItemRoteiro(
                UUID.randomUUID(),
                new Produto(substitutoId, "SKU-TIN-004", "Lixa d'Agua Grao 150",
                        new BigDecimal("4.20"), 40,
                        new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 50, 50)))));

        ServidorDeTeste.Resposta resposta = servidor.json("POST", item(itemId, "substituir"),
                "{\"produtoSubstitutoId\":\"" + substitutoId + "\"}");

        assertEquals(200, resposta.status());
        assertEquals(1, resposta.inteiro("quantidadeItens"));
        assertEquals("SKU-TIN-004", resposta.texto("itens[0].produto.sku"));
        assertEquals(false, resposta.json("itens[0].coletado"));
        assertEquals(itemId, substituir.chamadas.get(0)[0]);
        assertEquals(substitutoId, substituir.chamadas.get(0)[1]);
    }

    @Test
    @DisplayName("substituir sem o produto no corpo devolve 400 apontando o campo")
    void substituirSemProduto() throws Exception {
        ServidorDeTeste.Resposta resposta =
                servidor.json("POST", item(UUID.randomUUID(), "substituir"), "{}");

        assertEquals(400, resposta.status());
        assertEquals("produtoSubstitutoId", resposta.texto("validationErrors[0].field"));
        assertTrue(substituir.chamadas.isEmpty());
    }

    @Test
    @DisplayName("trocar um produto por ele mesmo devolve 409")
    void substituirPorSiMesmo() throws Exception {
        UUID produtoId = UUID.randomUUID();
        substituir.falha = new OperacaoNaoPermitidaException(
                "Produto %s nao pode substituir a si mesmo".formatted(produtoId));

        ServidorDeTeste.Resposta resposta = servidor.json("POST", item(UUID.randomUUID(), "substituir"),
                "{\"produtoSubstitutoId\":\"" + produtoId + "\"}");

        assertEquals(409, resposta.status());
    }

    @Test
    @DisplayName("item inexistente devolve 404")
    void itemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        marcar.falha = new RecursoNaoEncontradoException("Item do roteiro", itemId);

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("PATCH", item(itemId, "coletar"));

        assertEquals(404, resposta.status());
        assertEquals("Recurso Não Encontrado", resposta.texto("error"));
    }

    @Test
    @DisplayName("id malformado devolve 400")
    void idMalformado() throws Exception {
        assertEquals(400, servidor.semCorpo("PATCH", item("abc", "coletar")).status());
    }

    // ---------------------------------------------------------------- ruptura (UC-013)

    private RupturaEstoqueResponse sugestaoDaIA(UUID produtoOriginalId) {
        return RupturaEstoqueResponse.de(
                produtoOriginalId,
                new Produto(UUID.randomUUID(), "SKU-TIN-004",
                        "Lixa d'Agua Grao 150", new BigDecimal("4.20"), 40,
                        new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10)),
                "A lixa d'agua grao 150 da o mesmo acabamento e esta no mesmo corredor.",
                OrigemSugestao.ASSISTENTE_IA);
    }

    @Test
    @DisplayName("POST ruptura devolve o substituto com o corredor onde encontra-lo")
    void relatarRuptura() throws Exception {
        UUID produtoOriginalId = UUID.randomUUID();
        ruptura.resposta = sugestaoDaIA(produtoOriginalId);

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", item(UUID.randomUUID(), "ruptura"));

        assertEquals(200, resposta.status());
        assertEquals(produtoOriginalId.toString(), resposta.texto("produtoOriginalId"));
        assertEquals("SKU-TIN-004", resposta.texto("produtoSugerido.sku"));
        assertEquals(40, resposta.inteiro("produtoSugerido.saldoEstoque"));
        assertEquals("Tintas", resposta.texto("produtoSugerido.pontoMapa.corredor"));
        assertTrue(resposta.texto("justificativa").contains("acabamento"));
        assertEquals("ASSISTENTE_IA", resposta.texto("origemSugestao"));
    }

    @Test
    @DisplayName("a origem da sugestao chega ao frontend, que nao pode rotular fallback como IA")
    void expoeAOrigemDaSugestao() throws Exception {
        ruptura.resposta = RupturaEstoqueResponse.de(
                UUID.randomUUID(),
                new Produto(UUID.randomUUID(), "SKU-TIN-002",
                        "Rolo de La 23cm com Cabo", new BigDecimal("34.90"), 25,
                        new PontoMapa(UUID.randomUUID(), TipoPonto.PRATELEIRA, "Tintas", 32, 10)),
                "Este e o produto disponivel mais proximo de onde voce esta.",
                OrigemSugestao.PROXIMIDADE);

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", item(UUID.randomUUID(), "ruptura"));

        assertEquals(200, resposta.status());
        assertEquals("PROXIMIDADE", resposta.texto("origemSugestao"));
    }

    @Test
    @DisplayName("sem substituto plausivel devolve 422, distinguivel de 404")
    void semSubstitutoPlausivel() throws Exception {
        ruptura.falha = new SubstitutoIndisponivelException(
                "Nenhum substituto plausivel para 'Espelho Redondo 60cm' foi encontrado.");

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", item(UUID.randomUUID(), "ruptura"));

        assertEquals(422, resposta.status());
        assertEquals("Substituto Indisponível", resposta.texto("error"));
        assertTrue(resposta.texto("message").contains("Espelho Redondo 60cm"));
    }

    @Test
    @DisplayName("sessao encerrada devolve 409")
    void sessaoInativa() throws Exception {
        ruptura.falha = new OperacaoNaoPermitidaException("Sessao nao esta mais ativa (status EXPIRED)");

        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", item(UUID.randomUUID(), "ruptura"));

        assertEquals(409, resposta.status());
        assertEquals("Operação Não Permitida", resposta.texto("error"));
    }

    @Test
    @DisplayName("item inexistente na ruptura devolve 404")
    void rupturaDeItemInexistente() throws Exception {
        UUID itemId = UUID.randomUUID();
        ruptura.falha = new RecursoNaoEncontradoException("Item de roteiro", itemId);

        assertEquals(404, servidor.semCorpo("POST", item(itemId, "ruptura")).status());
    }

    @Test
    @DisplayName("id malformado na ruptura devolve 400, nao 500")
    void rupturaComIdMalformado() throws Exception {
        ServidorDeTeste.Resposta resposta = servidor.semCorpo("POST", item("nao-e-uuid", "ruptura"));

        assertEquals(400, resposta.status());
        assertEquals("Parâmetro Inválido", resposta.texto("error"));
    }

    @Test
    @DisplayName("GET no endereco da ruptura devolve 405, nao 500")
    void metodoErradoNaRuptura() throws Exception {
        assertEquals(405, servidor.get(item(UUID.randomUUID(), "ruptura")).status());
    }
}
