package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.PontoRotaResponse;
import br.com.jence.backend.application.dto.RotaCalculadaResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * UC-012: inclui um ponto de apoio (banheiro ou caixa) no trajeto em andamento.
 * <p>
 * O ponto de apoio <b>nao e persistido</b>: ele entra apenas na rota devolvida. Um banheiro
 * nao e produto e nao cabe como ItemRoteiro, e o DER nao tem tabela para isso. Como o celular
 * mantem a rota em cache, o desvio funciona normalmente - so nao sobrevive a uma recarga do
 * app, quando o cliente toca de novo. Ver D-31.
 */
@Service
@RequiredArgsConstructor
public class IncluirPontoDeInteresseUseCase {

    private final ListaRoteiroRepository listaRoteiroRepository;
    private final PontoMapaRepository pontoMapaRepository;

    public RotaCalculadaResponse executar(UUID sessaoId, TipoPonto tipo) {
        if (tipo != TipoPonto.BANHEIRO && tipo != TipoPonto.CAIXA) {
            throw new OperacaoNaoPermitidaException(
                    "Apenas BANHEIRO ou CAIXA podem ser incluidos como ponto de apoio, e nao %s".formatted(tipo));
        }

        ListaRoteiro lista = listaRoteiroRepository.buscarPorSessao(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));

        List<ItemRoteiro> itens = lista.getItensOrdenados();
        if (itens.isEmpty()) {
            throw new OperacaoNaoPermitidaException(
                    "Nao ha rota em andamento na sessao %s".formatted(sessaoId));
        }

        int posicaoAtual = indiceDaPosicaoAtual(itens);
        PontoMapa apoio = pontoDeApoioMaisProximoDe(referenciaDeDistancia(itens, posicaoAtual), tipo);

        return new RotaCalculadaResponse(
                lista.getSessaoId(),
                lista.getId(),
                montarSequenciaCom(itens, apoio, posicaoAtual)
        );
    }

    /*
     * O contrato envia apenas o tipo, sem coordenadas, entao a posicao do cliente e inferida
     * do que ele ja coletou: o ultimo item marcado indica ate onde ele chegou. Nada coletado
     * significa que a caminhada acabou de comecar.
     */
    private int indiceDaPosicaoAtual(List<ItemRoteiro> itens) {
        int ultimoColetado = -1;
        for (int i = 0; i < itens.size(); i++) {
            if (itens.get(i).isColetado()) {
                ultimoColetado = i;
            }
        }
        return ultimoColetado;
    }

    private PontoMapa referenciaDeDistancia(List<ItemRoteiro> itens, int posicaoAtual) {
        // Antes de coletar qualquer coisa, o cliente esta a caminho da primeira parada.
        int indice = posicaoAtual >= 0 ? posicaoAtual : 0;
        return itens.get(indice).getProduto().getPontoMapa();
    }

    private PontoMapa pontoDeApoioMaisProximoDe(PontoMapa referencia, TipoPonto tipo) {
        return pontoMapaRepository.buscarPorTipo(tipo).stream()
                .min(Comparator.comparingDouble(referencia::calcularDistanciaPara))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ponto de mapa do tipo", tipo));
    }

    /*
     * O desvio entra logo apos onde o cliente esta, antes da proxima compra. A ordem de
     * caminho gravada nos itens nao muda: o que se reordena e a sequencia de navegacao
     * devolvida, que agora tem uma parada a mais.
     */
    private List<PontoRotaResponse> montarSequenciaCom(List<ItemRoteiro> itens, PontoMapa apoio, int posicaoAtual) {
        List<PontoRotaResponse> sequencia = new ArrayList<>(itens.size() + 1);
        int ordem = 1;

        for (int i = 0; i < itens.size(); i++) {
            sequencia.add(PontoRotaResponse.de(itens.get(i), ordem++));
            if (i == posicaoAtual) {
                sequencia.add(PontoRotaResponse.dePontoDeApoio(apoio, ordem++));
            }
        }

        if (posicaoAtual < 0) {
            sequencia.add(0, PontoRotaResponse.dePontoDeApoio(apoio, 1));
            renumerar(sequencia);
        }

        return sequencia;
    }

    private void renumerar(List<PontoRotaResponse> sequencia) {
        for (int i = 0; i < sequencia.size(); i++) {
            PontoRotaResponse p = sequencia.get(i);
            sequencia.set(i, new PontoRotaResponse(i + 1, p.item(), p.pontoMapa()));
        }
    }
}
