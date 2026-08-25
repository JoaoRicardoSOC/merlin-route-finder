package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.PontoMapa;
import br.com.jence.backend.domain.entity.Produto;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProdutoRepository {

    Optional<Produto> buscarPorId(UUID id);

    Optional<Produto> buscarPorSku(String sku);

    Pagina<Produto> buscarPaginado(int pagina, int tamanho);

    /** Busca por nome, tolerante a busca parcial e a pequenos erros de digitacao. */
    Pagina<Produto> buscarPorTermo(String termo, int pagina, int tamanho);

    /**
     * A busca do catalogo, com todos os filtros da tela combinados.
     * <p>
     * E a unica implementacao de busca que existe: {@link #buscarPaginado} e
     * {@link #buscarPorTermo} delegam para ca com o filtro correspondente. Os dois continuam
     * na porta porque expressam intencoes diferentes - listar tudo para a carga, e buscar sem
     * filtro para fundamentar o assistente de IA.
     */
    Pagina<Produto> buscar(FiltroDeProdutos filtro, int pagina, int tamanho);

    /**
     * As secoes que tem produto hoje, com a contagem de cada uma.
     * <p>
     * Sai do catalogo, e nao da planta da loja: uma secao sem produto e um beco sem saida num
     * menu de navegacao, entao ela simplesmente nao aparece.
     */
    List<SecaoDoCatalogo> listarSecoes();

    /**
     * Pre-filtragem espacial do tratamento de ruptura (UC-013): produtos com saldo em estoque
     * dentro de um raio do ponto informado, do mais proximo para o mais distante.
     * <p>
     * A filtragem por proximidade acontece no banco, e nao em memoria, porque num catalogo
     * real trazer tudo para a aplicacao so para descartar a maior parte seria inviavel. E e
     * esta lista - e somente ela - que o assistente pode considerar ao eleger um substituto.
     *
     * @param referencia ponto do produto em falta
     * @param excluido   id do proprio produto em falta, que nao pode substituir a si mesmo
     */
    List<Produto> buscarDisponiveisProximosDe(PontoMapa referencia, UUID excluido, double raio, int limite);

    Produto salvar(Produto produto);
}
