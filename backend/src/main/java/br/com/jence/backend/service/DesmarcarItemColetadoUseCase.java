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
import java.util.Optional;
import java.util.UUID;

/**
 * O cliente desfaz uma coleta: tocou por engano, ou devolveu o produto a prateleira.
 * <p>
 * <b>Existe porque tocar sem querer num celular, andando, e comum</b> - e ate agora nao havia
 * volta. Um item marcado por engano ficava marcado, e ainda arrastava a posicao do cliente
 * para uma prateleira onde ele nunca esteve.
 * <p>
 * Espelho de {@link MarcarItemColetadoUseCase}, inclusive na renovacao da sessao: corrigir um
 * engano e atividade do cliente como qualquer outra.
 */
@Service
public class DesmarcarItemColetadoUseCase {

    private final ListaRoteiroDao listaRoteiroDao;
    private final SessaoDao sessaoDao;

    public DesmarcarItemColetadoUseCase(ListaRoteiroDao listaRoteiroDao, SessaoDao sessaoDao) {
        this.listaRoteiroDao = listaRoteiroDao;
        this.sessaoDao = sessaoDao;
    }

    public ItemRoteiroDetalhadoResponse executar(UUID itemId) throws SQLException, RecursoNaoEncontradoException {
        try (Transacao transacao = Transacao.iniciar()) {
            ItemRoteiroDetalhadoResponse resposta = desmarcar(itemId);
            transacao.confirmar();
            return resposta;
        }
    }

    private ItemRoteiroDetalhadoResponse desmarcar(UUID itemId) throws SQLException, RecursoNaoEncontradoException {
        // Pela raiz do agregado, e nao pelo item isolado: e por ela que se chega a sessao.
        ListaRoteiro lista = listaRoteiroDao.buscarPorItem(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        ItemRoteiro item = lista.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        /*
         * Nao ha nada a fazer com a posicao aqui. Ela e deduzida do item coletado mais
         * recente, entao apagar o instante ja a devolve ao item marcado antes deste - ou a
         * placa lida, se nao houver nenhum. Ver D-64.
         */
        item.desmarcarComoColetado();
        listaRoteiroDao.salvar(lista);

        Optional<Sessao> sessao = sessaoDao.buscarPorId(lista.getSessaoId());
        if (sessao.isPresent()) {
            sessao.get().renovarSessao();
            sessaoDao.salvar(sessao.get());
        }

        return ItemRoteiroDetalhadoResponse.de(item);
    }
}
