package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.PontoMapaRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UC-001: inicializa a sessao quando o cliente entra pela placa de localizacao.
 * <p>
 * O codigo da placa chega das duas formas previstas no fluxo: embutido na URL do QR Code, ou
 * digitado pelo cliente quando escanear nao deu certo. Para o caso de uso e o mesmo caminho.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InicializarSessaoUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;
    private final PontoMapaRepository pontoMapaRepository;

    /*
     * Transacional porque a sessao e a lista sao gravadas em duas chamadas: se a segunda
     * falhasse, restaria uma sessao sem lista e o proximo passo da jornada (adicionar
     * produto ao roteiro) quebraria com um erro dificil de diagnosticar.
     */
    @Transactional
    public SessaoResponse executar(String codigoPonto) {
        /*
         * Codigo invalido nao recusa a sessao: placa velha, loja remanejada ou erro de
         * digitacao deixam o cliente sem "voce esta aqui", mas com o sistema inteiro
         * funcionando. Recusar aqui seria barrar a entrada por causa de um adesivo.
         */
        PontoMapa ponto = pontoMapaRepository.buscarPorCodigoCurto(codigoPonto).orElse(null);

        if (ponto == null && codigoPonto != null && !codigoPonto.isBlank()) {
            log.info("Sessao iniciada com codigo de placa desconhecido: {}. "
                    + "Nasce sem posicao.", codigoPonto);
        }

        Sessao sessao = sessaoRepository.salvar(Sessao.iniciar(UUID.randomUUID(), ponto));

        listaRoteiroRepository.salvar(ListaRoteiro.criarPara(UUID.randomUUID(), sessao.getId()));

        return SessaoResponse.de(sessao);
    }
}
