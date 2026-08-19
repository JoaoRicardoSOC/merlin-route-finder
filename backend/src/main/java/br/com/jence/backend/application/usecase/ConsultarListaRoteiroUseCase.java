package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.ListaRoteiroResponse;
import br.com.jence.backend.domain.exception.RecursoNaoEncontradoException;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * UC-005: consulta a lista de compras montada na sessao.
 * <p>
 * Nao renova o TTL da sessao: consultar e uma leitura e nao deveria gravar nada. A renovacao
 * acontece nas acoes que de fato indicam o cliente montando a lista (adicionar e remover).
 */
@Service
@RequiredArgsConstructor
public class ConsultarListaRoteiroUseCase {

    private final ListaRoteiroRepository listaRoteiroRepository;

    public ListaRoteiroResponse executar(UUID sessaoId) {
        return listaRoteiroRepository.buscarPorSessao(sessaoId)
                .map(ListaRoteiroResponse::de)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Lista de roteiro da sessao", sessaoId));
    }
}
