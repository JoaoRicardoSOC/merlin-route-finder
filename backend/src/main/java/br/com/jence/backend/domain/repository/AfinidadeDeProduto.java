package br.com.jence.backend.domain.repository;

import br.com.jence.backend.domain.entity.AtributoProduto;
import br.com.jence.backend.domain.entity.ValorDeAtributo;

import java.util.List;

/**
 * O que faz um produto se parecer com outro, para efeito de substituicao.
 * <p>
 * <b>A ordem dos campos e a ordem da preferencia.</b> Duas lixas continuam sendo lixas mesmo
 * de marcas diferentes; duas Norton podem ser uma lixa e uma serra. Por isso <b>tipo pesa mais
 * que marca</b>.
 * <p>
 * Existe porque a pre-filtragem espacial sozinha deixou de bastar quando o catalogo cresceu:
 * todos os produtos de uma secao compartilham a coordenada do bloco, empatam em distancia, e o
 * desempate acabava sendo o nome - o que transformava "o mais proximo" em "o primeiro do
 * corredor em ordem alfabetica". Ver D-68.
 *
 * @param tipo  o que o produto e ("Lixa para parede", "Trena", "Argamassa AC-II")
 * @param marca fabricante
 */
public record AfinidadeDeProduto(String tipo, String marca) {

    /** Sem afinidade conhecida: a ordenacao cai de volta para distancia e nome. */
    public static AfinidadeDeProduto nenhuma() {
        return new AfinidadeDeProduto(null, null);
    }

    public static AfinidadeDeProduto de(List<ValorDeAtributo> caracteristicas) {
        if (caracteristicas == null) {
            return nenhuma();
        }
        return new AfinidadeDeProduto(
                valorDe(caracteristicas, AtributoProduto.TIPO),
                valorDe(caracteristicas, AtributoProduto.MARCA));
    }

    private static String valorDe(List<ValorDeAtributo> caracteristicas, AtributoProduto qual) {
        return caracteristicas.stream()
                .filter(caracteristica -> caracteristica.atributo() == qual)
                .map(ValorDeAtributo::valor)
                .findFirst()
                .orElse(null);
    }
}
