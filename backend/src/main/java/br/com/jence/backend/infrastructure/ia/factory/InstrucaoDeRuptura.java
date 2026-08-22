package br.com.jence.backend.infrastructure.ia.factory;

import br.com.jence.backend.domain.service.FerramentaIA;

import java.util.List;

/**
 * Instrucao de sistema e ferramenta do tratamento de ruptura de estoque (UC-013).
 * <p>
 * Separada da {@link InstrucaoDoAssistente} porque as duas conversas nao se parecem: o chat e
 * aberto e multi-turno, este e um julgamento unico com resposta em formato fixo.
 */
public final class InstrucaoDeRuptura {

    public static final String FERRAMENTA_BUSCAR_SUBSTITUTOS = "buscar_substitutos_proximos";

    /** Resposta do modelo quando nenhum candidato serve ao mesmo proposito. */
    public static final String NENHUM = "NENHUM";

    /** Separador entre o SKU escolhido e a justificativa. */
    public static final String SEPARADOR = "|";

    private InstrucaoDeRuptura() {
    }

    /*
     * O formato fixo de resposta nao e o que garante o grounding - a validacao do SKU contra a
     * lista de candidatos e. O formato existe so para tornar a escolha legivel pelo sistema; o
     * produto entregue ao cliente e sempre resolvido no nosso banco a partir do SKU, e o texto
     * do modelo sobrevive apenas como justificativa. Ver D-38.
     */
    public static String instrucaoDeSistema(String produtoEmFalta, String corredor) {
        return """
                Voce ajuda um cliente que esta neste momento em pe diante de uma prateleira vazia
                numa loja Leroy Merlin. O produto que ele veio buscar e:

                    "%s" (corredor %s)

                O produto acabou na gondola. Sua tarefa e escolher UM substituto entre os produtos
                que a loja tem em estoque fisicamente perto dali.

                COMO PROCEDER:

                1. Chame a ferramenta %s para obter a lista de candidatos. Ela ja vem filtrada por
                   proximidade e disponibilidade - voce nao precisa e nao deve procurar em outro
                   lugar.

                2. Escolha o candidato que melhor cumpre a MESMA FUNCAO do produto em falta. Um
                   produto que serve para outra finalidade nao e substituto, por mais perto que
                   esteja. Entre dois que servem, prefira o mais proximo.

                3. Se nenhum candidato cumprir a mesma funcao, diga isso em vez de improvisar.
                   Sugerir algo inadequado e pior do que nao sugerir nada.

                FORMATO OBRIGATORIO DA RESPOSTA - uma unica linha, sem markdown, sem texto antes
                ou depois:

                    SKU %s justificativa

                Onde SKU e exatamente o codigo do candidato escolhido, e a justificativa e uma ou
                duas frases em portugues do Brasil, faladas diretamente ao cliente, dizendo por
                que aquele produto resolve e onde ele esta. Nao repita o codigo na justificativa.

                Se nenhum servir, responda:

                    %s %s o motivo em uma frase

                Exemplo de resposta valida:
                SKU-TIN-004 %s A lixa d'agua grao 150 da o mesmo acabamento e esta no mesmo
                corredor, poucos metros a frente.
                """.formatted(produtoEmFalta, corredor, FERRAMENTA_BUSCAR_SUBSTITUTOS,
                SEPARADOR, NENHUM, SEPARADOR, SEPARADOR);
    }

    /*
     * A ferramenta nao recebe parametros de proposito. Quais produtos entram na lista e
     * decisao do sistema, tomada pela consulta espacial no banco - se o modelo pudesse
     * informar o corredor ou o termo de busca, ele estaria escolhendo o proprio universo de
     * opcoes, que e justamente o que a pre-filtragem existe para impedir.
     */
    public static List<FerramentaIA> ferramentas() {
        return List.of(new FerramentaIA(
                FERRAMENTA_BUSCAR_SUBSTITUTOS,
                "Devolve os produtos que a loja tem em estoque fisicamente mais proximos do "
                        + "produto em falta, com codigo (sku), nome, preco, corredor e a distancia "
                        + "ate a prateleira onde o cliente esta - quanto menor, mais perto.",
                List.of()));
    }
}
