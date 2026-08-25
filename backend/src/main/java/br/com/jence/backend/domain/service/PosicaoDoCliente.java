package br.com.jence.backend.domain.service;

import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Sessao;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Optional;

/**
 * Onde o cliente esta, pela melhor evidencia que o sistema tem.
 * <p>
 * Sem GPS dentro da loja, a posicao nunca e medida - e sempre deduzida de algo que o cliente
 * fez. Existem duas pistas, e nenhuma delas e melhor que a outra em absoluto:
 * <ul>
 *   <li><b>a placa lida</b> - ele estava ali quando escaneou ou digitou o codigo;</li>
 *   <li><b>o ultimo item coletado</b> - ele esteve na prateleira daquele produto.</li>
 * </ul>
 * <p>
 * <b>Vale a mais recente.</b> Comparar as duas por data e o que faz o recentrar funcionar:
 * quem se perdeu e leu uma placa nova espera ver o marcador ali, e nao na prateleira do
 * ultimo item que pegou. Sem a comparacao, a placa nova seria ignorada.
 * <p>
 * Regra de negocio pura: nao conhece banco, HTTP nem framework.
 */
public final class PosicaoDoCliente {

    private PosicaoDoCliente() {
    }

    /**
     * @return o ponto onde o cliente provavelmente esta, ou vazio quando nao ha pista nenhuma
     *         - sessao sem placa e sem nada coletado ainda
     */
    public static Optional<PontoMapa> estimar(Sessao sessao, ListaRoteiro lista) {
        Optional<ItemRoteiro> ultimoColetado = ultimoColetado(lista);

        if (ultimoColetado.isEmpty()) {
            return Optional.ofNullable(sessao.getPontoEscaneado());
        }
        if (sessao.getEscaneadoEm() == null) {
            return ultimoColetado.map(item -> item.getProduto().getPontoMapa());
        }

        LocalDateTime coleta = ultimoColetado.get().getColetadoEm();

        return sessao.getEscaneadoEm().isAfter(coleta)
                ? Optional.ofNullable(sessao.getPontoEscaneado())
                : ultimoColetado.map(item -> item.getProduto().getPontoMapa());
    }

    private static Optional<ItemRoteiro> ultimoColetado(ListaRoteiro lista) {
        if (lista == null) {
            return Optional.empty();
        }
        return lista.getItens().stream()
                .filter(ItemRoteiro::isColetado)
                .max(Comparator.comparing(ItemRoteiro::getColetadoEm));
    }
}
