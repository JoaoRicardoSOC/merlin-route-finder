package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.OperacaoNaoPermitidaException;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O cliente se perdeu e leu outra placa da loja.
 * <p>
 * Nao cria sessao nova: a lista que ele montou continua inteira, com o que ja foi coletado
 * preservado. Muda apenas onde ele esta.
 */
@Service
@RequiredArgsConstructor
public class RecentrarSessaoUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;
    private final PontoMapaRepository pontoMapaRepository;

    @Transactional
    public SessaoResponse executar(UUID sessaoId, String codigoPonto) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        if (!sessao.isValida()) {
            throw new OperacaoNaoPermitidaException(
                    "Sessao %s nao esta mais ativa (status %s)".formatted(sessaoId, sessao.getStatus()));
        }

        /*
         * Aqui codigo desconhecido e erro, ao contrario da criacao da sessao, que aceita e
         * nasce sem posicao. A diferenca e o custo de recusar: na entrada, recusar barraria o
         * cliente do sistema inteiro por causa de um adesivo; aqui ele ja tem sessao
         * funcionando, e avisar que a placa nao foi encontrada e acionavel - ele tenta de
         * novo. Aceitar em silencio seria pior: ele acharia que funcionou. Ver D-57.
         */
        PontoMapa ponto = pontoMapaRepository.buscarPorCodigoCurto(codigoPonto)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Placa de localizacao", codigoPonto));

        sessao.recentrarEm(ponto, LocalDateTime.now());

        // Quem para para ler uma placa esta ativo na loja, e nao pode perder a sessao por
        // inatividade logo depois de pedir ajuda para se achar.
        sessao.renovarSessao();
        sessaoRepository.salvar(sessao);

        return SessaoResponse.de(sessao,
                listaRoteiroRepository.buscarPorSessao(sessaoId).orElse(null));
    }
}
