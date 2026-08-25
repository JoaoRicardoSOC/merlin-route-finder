package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.SecaoResponse;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/** As secoes da loja, para o menu de navegacao do catalogo. */
@Service
@RequiredArgsConstructor
public class ListarSecoesUseCase {

    private final ProdutoRepository produtoRepository;

    public List<SecaoResponse> executar() {
        return produtoRepository.listarSecoes().stream()
                .map(SecaoResponse::de)
                .toList();
    }
}
