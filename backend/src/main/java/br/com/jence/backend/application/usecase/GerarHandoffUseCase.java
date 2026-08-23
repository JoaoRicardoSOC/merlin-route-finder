package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.HandoffResponse;
import br.com.jence.backend.domain.entity.ItemRoteiro;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.entity.TipoPonto;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import br.com.jence.backend.domain.service.CalculadoraRota;
import br.com.jence.backend.domain.service.GeradorTokenHandoff;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * UC-010: conclui a montagem da lista no Totem e prepara a transicao para o celular.
 * <p>
 * Calcula a rota, assina o token que sera lido do QR Code e persiste ambos.
 */
@Service
@RequiredArgsConstructor
public class GerarHandoffUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;
    private final PontoMapaRepository pontoMapaRepository;
    private final GeradorTokenHandoff geradorTokenHandoff;

    @Value("${merlin.handoff.base-url}")
    private String baseUrlMobile;

    @Transactional
    public HandoffResponse executar(UUID sessaoId) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        ListaRoteiro lista = listaRoteiroRepository.buscarPorSessao(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));

        if (lista.isVazia()) {
            throw new OperacaoNaoPermitidaException(
                    "Nao ha o que roteirizar: a lista da sessao %s esta vazia".formatted(sessaoId));
        }

        /*
         * Chamar este caso de uso de novo e o caminho de regeneracao do QR Code (D-44): o
         * token anterior deixa de ser encontravel e um novo passa a valer.
         *
         * Mas so recalcula a rota se a caminhada ainda nao comecou. Se o cliente ja coletou
         * algum item, ele esta no meio da loja: recalcular partindo do totem renumeraria
         * paradas ja visitadas e embaralharia a navegacao dele. Regenerar precisa devolver o
         * acesso, nao reiniciar o percurso.
         */
        if (caminhadaNaoComecou(lista)) {
            /*
             * CalculadoraRota grava a ordem em cada ItemRoteiro. getItens() devolve copia da
             * lista, mas com as mesmas referencias de item, entao a ordem atinge o agregado
             * real e e persistida junto com a lista logo abaixo.
             */
            CalculadoraRota.calcularRota(localizarTotem(), lista.getItens());
        }

        String token = geradorTokenHandoff.gerar(lista.getId(), sessaoId);
        lista.registrarTokenHandoff(token);
        listaRoteiroRepository.salvar(lista);

        // O cliente ainda vai percorrer a loja: a sessao precisa sobreviver a caminhada.
        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        return new HandoffResponse(
                "%s/rota?token=%s".formatted(baseUrlMobile, token),
                token,
                lista.getTokenExpiracao()
        );
    }

    private boolean caminhadaNaoComecou(ListaRoteiro lista) {
        return lista.getItens().stream().noneMatch(ItemRoteiro::isColetado);
    }

    /*
     * A rota parte do totem onde o cliente esta. Numa loja com varios totens seria preciso o
     * Totem informar qual e ele na requisicao; o contrato hoje envia apenas a sessao, entao
     * assumimos o primeiro cadastrado (ver D-28).
     */
    private PontoMapa localizarTotem() {
        List<PontoMapa> totens = pontoMapaRepository.buscarPorTipo(TipoPonto.TOTEM);
        if (totens.isEmpty()) {
            throw new RecursoNaoEncontradoException("Ponto de mapa do tipo", TipoPonto.TOTEM);
        }
        return totens.get(0);
    }
}
