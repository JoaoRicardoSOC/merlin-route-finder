package br.com.jence.backend.service;

import br.com.jence.backend.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.model.ItemRoteiro;
import br.com.jence.backend.model.ListaRoteiro;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.dao.ListaRoteiroDao;
import br.com.jence.backend.dao.SessaoDao;
import br.com.jence.backend.factory.Transacao;
import br.com.jence.backend.model.Sessao;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * UC-014 (parte 1): o cliente confirma ter pego um produto da prateleira.
 * <p>
 * E o que faz o marcador do item mudar de aparencia no mapa, e tambem <b>a principal pista de
 * onde o cliente esta</b>: quem acabou de pegar um produto esta na prateleira dele. Ver
 * {@link br.com.jence.backend.service.PosicaoDoCliente}.
 */
@Service
public class MarcarItemColetadoUseCase {

    private final ListaRoteiroDao listaRoteiroDao;
    private final SessaoDao sessaoDao;

    public MarcarItemColetadoUseCase(ListaRoteiroDao listaRoteiroDao, SessaoDao sessaoDao) {
        this.listaRoteiroDao = listaRoteiroDao;
        this.sessaoDao = sessaoDao;
    }

    public ItemRoteiroDetalhadoResponse executar(UUID itemId) throws SQLException {
        try (Transacao transacao = Transacao.iniciar()) {
            ItemRoteiroDetalhadoResponse resposta = marcar(itemId);
            transacao.confirmar();
            return resposta;
        }
    }

    private ItemRoteiroDetalhadoResponse marcar(UUID itemId) throws SQLException {
        /*
         * Carrega pela raiz do agregado em vez de alterar o item isolado: e por ela que se
         * chega a sessao, que precisa ser renovada abaixo.
         */
        ListaRoteiro lista = listaRoteiroDao.buscarPorItem(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        ItemRoteiro item = lista.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        // Idempotente: tocar duas vezes, ou a rede reenviar, nao e erro - e a hora que vale
        // continua sendo a da primeira confirmacao, para nao mover a posicao do cliente.
        item.marcarComoColetado(LocalDateTime.now());
        listaRoteiroDao.salvar(lista);

        /*
         * Sem renovar aqui, a sessao morreria no meio da
         * caminhada de quem tem lista grande numa loja de 10.000m2, quebrando a marcacao dos
         * itens seguintes e o tratamento de ruptura.
         */
        Optional<Sessao> sessao = sessaoDao.buscarPorId(lista.getSessaoId());
        if (sessao.isPresent()) {
            sessao.get().renovarSessao();
            sessaoDao.salvar(sessao.get());
        }

        return ItemRoteiroDetalhadoResponse.de(item);
    }
}
