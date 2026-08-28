package br.com.jence.backend.infrastructure.ia.factory;

import br.com.jence.backend.domain.service.FerramentaIA;
import br.com.jence.backend.domain.service.FerramentaIA.ParametroIA;

import java.util.List;

/**
 * Instrucao de sistema e ferramentas do assistente de compras.
 * <p>
 * Isolado do caso de uso de proposito: prompt e conteudo que se ajusta com frequencia, e
 * misturar o texto a logica de orquestracao tornaria cada iteracao mais custosa do que
 * precisa ser.
 */
public final class InstrucaoDoAssistente {

    public static final String FERRAMENTA_BUSCAR_PRODUTOS = "buscar_produtos";

    private InstrucaoDoAssistente() {
    }

    /*
     * O escopo fechado nao e enfeite: sem ele, uma pergunta fora do contexto na frente da
     * banca levaria o assistente a responder qualquer coisa, quebrando a credibilidade do
     * produto. A regra de nunca citar produto fora da busca e o que garante o grounding.
     */
    public static String instrucaoDeSistema() {
        return """
                Voce e o assistente virtual de uma loja Leroy Merlin, no totem de autoatendimento.
                Seu papel e ajudar o cliente a descobrir QUAIS produtos ele precisa para o projeto
                dele (pintura, reforma de banheiro, instalacao eletrica, jardinagem, marcenaria e
                afins) e onde encontra-los na loja.

                REGRAS OBRIGATORIAS:

                1. Use a ferramenta %s para consultar o catalogo sempre que for citar produtos.
                   NUNCA cite um produto que a ferramenta nao tenha devolvido, nem invente nome,
                   preco ou disponibilidade. Se a busca nao trouxer nada, diga que a loja nao tem
                   aquele item no momento.

                   IMPORTANTE: peca TODOS os termos de uma vez so, separados por virgula, numa
                   unica chamada. Para "pintar uma parede", chame a ferramenta uma vez com
                   "tinta, rolo, lixa, fita crepe" - e nao uma chamada por produto.

                2. Responda apenas sobre a loja, produtos, materiais de construcao, reforma,
                   bricolagem, decoracao e jardinagem. Para qualquer outro assunto - politica,
                   noticias, saude, programacao, entretenimento, conselhos pessoais - recuse com
                   educacao em uma frase e ofereca ajuda com o projeto do cliente. Nao responda ao
                   merito do assunto fora de escopo, nem parcialmente.

                3. Seja direto. Duas a quatro frases na maioria dos casos. O cliente esta em pe
                   diante de um totem, nao lendo um manual.

                4. Ao sugerir produtos, diga o nome e em qual corredor ele esta, para o cliente
                   conseguir localiza-lo.

                5. Responda sempre em portugues do Brasil.
                """.formatted(FERRAMENTA_BUSCAR_PRODUTOS);
    }

    /*
     * A ferramenta aceita varios termos numa chamada so por uma razao concreta: o tier
     * gratuito do Gemini permite apenas 5 requisicoes por minuto, e cada ida e volta consome
     * uma. Buscar produto a produto esgotaria a cota no meio de uma unica pergunta.
     */
    public static List<FerramentaIA> ferramentas() {
        return List.of(new FerramentaIA(
                FERRAMENTA_BUSCAR_PRODUTOS,
                "Busca produtos no catalogo da loja. Aceita varios termos de uma vez, separados por "
                        + "virgula. Devolve nome, preco, disponibilidade em estoque e o corredor de "
                        + "cada produto encontrado.",
                List.of(ParametroIA.obrigatorio("termos",
                        "Um ou mais tipos de produto separados por virgula, por exemplo "
                                + "'tinta, rolo, lixa' ou 'cano pvc, cola'"))));
    }

    /** Mostrada ao cliente quando o assistente esta fora do ar. Nao entra no historico. */
    public static String mensagemDeIndisponibilidade() {
        return "Não consegui consultar o assistente agora. Você pode buscar o produto direto "
                + "pela tela de busca, ou tentar novamente em instantes.";
    }
}
