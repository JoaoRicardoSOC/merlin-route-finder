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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * UC-014 (parte 1): o cliente confirma ter pego um produto da prateleira.
 * <p>
 * E o que faz o marcador do item mudar de aparencia no mapa, e tambem <b>a principal pista de
 * onde o cliente esta</b>: quem acabou de pegar um produto esta na prateleira dele. Ver
 * {@link br.com.jence.backend.domain.service.PosicaoDoCliente}.
 */
@Service
@RequiredArgsConstructor
public class MarcarItemColetadoUseCase {

    private final ListaRoteiroRepository listaRoteiroRepository;
    private final SessaoRepository sessaoRepository;

    @Transactional
    public ItemRoteiroDetalhadoResponse executar(UUID itemId) {
        /*
         * Carrega pela raiz do agregado em vez de alterar o item isolado: e por ela que se
         * chega a sessao, que precisa ser renovada abaixo.
         */
        ListaRoteiro lista = listaRoteiroRepository.buscarPorItem(itemId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        ItemRoteiro item = lista.getItens().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new RecursoNaoEncontradoException("Item do roteiro", itemId));

        // Idempotente: tocar duas vezes, ou a rede reenviar, nao e erro - e a hora que vale
        // continua sendo a da primeira confirmacao, para nao mover a posicao do cliente.
        item.marcarComoColetado(LocalDateTime.now());
        listaRoteiroRepository.salvar(lista);

        /*
         * Sem renovar aqui, a sessao morreria no meio da
         * caminhada de quem tem lista grande numa loja de 10.000m2, quebrando a marcacao dos
         * itens seguintes e o tratamento de ruptura.
         */
        sessaoRepository.buscarPorId(lista.getSessaoId()).ifPresent(sessao -> {
            sessao.renovarSessao();
            sessaoRepository.salvar(sessao);
        });

        return ItemRoteiroDetalhadoResponse.de(item);
    }
}
