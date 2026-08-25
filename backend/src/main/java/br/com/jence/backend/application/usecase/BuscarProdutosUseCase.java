package br.com.jence.backend.application.usecase;

import br.com.jence.backend.application.dto.CatalogoResponse;
import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.Produto;

import java.util.List;
import java.util.Map;
import br.com.jence.backend.domain.repository.FiltroDeProdutos;
import br.com.jence.backend.domain.repository.Pagina;
import br.com.jence.backend.domain.repository.ProdutoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * UC-002: busca e filtragem do catalogo.
 * <p>
 * Um caminho so para navegar e para buscar: sem termo, o cliente esta navegando; com termo,
 * buscando. Os filtros de secao e disponibilidade valem nos dois casos.
 */
@Service
@RequiredArgsConstructor
public class BuscarProdutosUseCase {

    static final int TAMANHO_PADRAO = 20;
    static final int TAMANHO_MAXIMO = 100;

    private final ProdutoRepository produtoRepository;

    public CatalogoResponse executar(String termo, String secao, Boolean apenasDisponiveis,
                                     Map<AtributoProduto, List<String>> atributos,
                                     Integer pagina, Integer tamanho) {
        int paginaSegura = Math.max(0, pagina == null ? 0 : pagina);
        int tamanhoSeguro = normalizarTamanho(tamanho);

        /*
         * Termo ou secao em branco significam "nao filtre por isso", e nao "filtre por vazio".
         * O FiltroDeProdutos e quem normaliza, entao a regra vale para todo mundo que buscar.
         */
        FiltroDeProdutos filtro = new FiltroDeProdutos(
                termo, secao, Boolean.TRUE.equals(apenasDisponiveis), atributos);

        /*
         * Combinacao sem resultado devolve pagina vazia, e nao 404: o cliente filtrou demais,
         * nao pediu um recurso que nao existe. A tela mostra "nenhum produto encontrado" e os
         * filtros continuam la para ele afrouxar um.
         */
        Pagina<Produto> resultado = produtoRepository.buscar(filtro, paginaSegura, tamanhoSeguro);

        /*
         * As facetas saem do recorte SEM as caracteristicas escolhidas. Calcula-las sobre o
         * resultado final faria as outras marcas sumirem assim que o cliente escolhesse uma -
         * e para trocar de ideia ele teria que limpar o filtro antes. Ver D-63.
         */
        return CatalogoResponse.de(
                resultado, produtoRepository.calcularFacetas(filtro.semAtributos()));
    }

    /* Limita o tamanho para que um size absurdo na URL nao carregue o catalogo inteiro em memoria. */
    private int normalizarTamanho(Integer tamanho) {
        if (tamanho == null || tamanho < 1) {
            return TAMANHO_PADRAO;
        }
        return Math.min(tamanho, TAMANHO_MAXIMO);
    }
}
