package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.domain.entity.ListaRoteiro;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * UC-001: inicializa a sessao do cliente quando ele comeca a interagir com o Totem.
 */
@Service
@RequiredArgsConstructor
public class InicializarSessaoUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;

    /*
     * Transacional porque a sessao e a lista sao gravadas em duas chamadas: se a segunda
     * falhasse, restaria uma sessao sem lista e o proximo passo da jornada (adicionar
     * produto ao roteiro) quebraria com um erro dificil de diagnosticar.
     */
    @Transactional
    public SessaoResponse executar() {
        Sessao sessao = sessaoRepository.salvar(Sessao.iniciar(UUID.randomUUID()));

        listaRoteiroRepository.salvar(ListaRoteiro.criarPara(UUID.randomUUID(), sessao.getId()));

        return SessaoResponse.de(sessao);
    }
}
