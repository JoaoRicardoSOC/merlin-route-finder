package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SessaoResponse;
import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Consulta o estado atual da sessao.
 * <p>
 * E por aqui que o celular descobre <b>onde o cliente esta</b> ao reabrir a pagina, alem de
 * saber se a sessao continua valida. Por isso carrega tambem a lista: a posicao pode vir do
 * ultimo item coletado, e nao apenas da placa lida.
 */
@Service
@RequiredArgsConstructor
public class ConsultarSessaoUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;

    @Transactional(readOnly = true)
    public SessaoResponse executar(UUID sessaoId) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        return SessaoResponse.de(sessao,
                listaRoteiroRepository.buscarPorSessao(sessaoId).orElse(null));
    }
}
