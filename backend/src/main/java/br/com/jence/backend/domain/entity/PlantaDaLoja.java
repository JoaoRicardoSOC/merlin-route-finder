package br.com.jence.backend.domain.entity;

import java.util.List;

/**
 * A planta da loja: onde fica cada corredor, no grid 0-100.
 * <p>
 * <b>Nao vive no banco de proposito.</b> A planta descreve um predio, nao um dado de
 * aplicacao: nao muda em execucao, ninguem a referencia por chave estrangeira e nada no
 * sistema a escreve. Persisti-la custaria entidade, factory, porta, adapter e carga para
 * entregar exatamente o mesmo conteudo - e mudar a planta continuaria sendo mudar codigo.
 * Ver D-58.
 * <p>
 * <b>E a fonte das coordenadas das secoes.</b> A carga inicial cria cada ponto de prateleira
 * no centro do bloco correspondente, entao um produto nao tem como aparecer fora do proprio
 * corredor no mapa. Acrescentar uma secao a massa exige acrescentar um bloco aqui.
 * <p>
 * As posicoes seguem aproximadamente a planta real compartilhada pela Leroy no kickoff.
 */
public final class PlantaDaLoja {

    private static final List<BlocoMapa> BLOCOS = List.of(
            //             rotulo                      x   y   larg  alt      centro
            new BlocoMapa("Tintas",                    20,  4,  24,  12),  // (32, 10)
            new BlocoMapa("Ferragens",                 17, 24,  10,  16),  // (22, 32)
            new BlocoMapa("Eletrica",                  29, 22,  10,  16),  // (34, 30)
            new BlocoMapa("Encanamento",               42, 22,  12,  16),  // (48, 30)
            new BlocoMapa("Cozinhas",                  56, 22,  12,  16),  // (62, 30)
            new BlocoMapa("Iluminacao",                70, 24,  12,  16),  // (76, 32)
            new BlocoMapa("Jardim",                    27, 44,  18,  12),  // (36, 50)
            new BlocoMapa("Ferramentas",               14, 48,  12,  14),  // (20, 55)
            new BlocoMapa("Decoracao",                 80, 47,  16,  16),  // (88, 55)
            new BlocoMapa("Materiais de construcao",    4, 73,  20,  14)); // (14, 80)

    private PlantaDaLoja() {
    }

    public static List<BlocoMapa> blocos() {
        return BLOCOS;
    }
}
