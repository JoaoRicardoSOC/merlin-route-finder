package br.com.jence.backend.service;

import br.com.jence.backend.dto.SessaoResponse;
import br.com.jence.backend.model.Sessao;
import br.com.jence.backend.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
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
public class ConsultarSessaoUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;

    public ConsultarSessaoUseCase(SessaoRepository sessaoRepository,
                                  ListaRoteiroRepository listaRoteiroRepository) {
        this.sessaoRepository = sessaoRepository;
        this.listaRoteiroRepository = listaRoteiroRepository;
    }

    @Transactional(readOnly = true)
    public SessaoResponse executar(UUID sessaoId) {
        Sessao sessao = sessaoRepository.buscarPorId(sessaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessao", sessaoId));

        return SessaoResponse.de(sessao,
                listaRoteiroRepository.buscarPorSessao(sessaoId).orElse(null));
    }
}
