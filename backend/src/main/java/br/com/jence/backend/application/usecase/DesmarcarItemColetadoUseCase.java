package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ItemRoteiroDetalhadoResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@RequiredArgsConstructor
public class DesmarcarItemColetadoUseCase {

    private final ListaRoteiroRepository listaRoteiroRepository;
    private final SessaoRepository sessaoRepository;

    @Transactional
    public ItemRoteiroDetalhadoResponse executar(UUID itemId) {
        // Pela raiz do agregado, e nao pelo item isolado: e por ela que se chega a sessao.
        ListaRoteiro lista = listaRoteiroRepository.buscarPorItem(itemId)
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
        listaRoteiroRepository.salvar(lista);

        sessaoRepository.buscarPorId(lista.getSessaoId()).ifPresent(sessao -> {
            sessao.renovarSessao();
            sessaoRepository.salvar(sessao);
        });

        return ItemRoteiroDetalhadoResponse.de(item);
    }
}
