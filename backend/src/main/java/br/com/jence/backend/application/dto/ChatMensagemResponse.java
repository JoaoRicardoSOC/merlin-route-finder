package br.com.jence.backend.application.dto;

import br.com.jence.backend.domain.entity.ChatMensagem;
import br.com.jence.backend.domain.entity.Remetente;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/** Espelha o schema {@code ChatMensagem} do contrato OpenAPI. */
public record ChatMensagemResponse(
        UUID id,
        UUID sessaoId,
        Remetente remetente,
        String conteudo,
        LocalDateTime enviadoEm,

        /**
         * Os produtos que o assistente citou nesta resposta, ou lista vazia.
         *
         * <p><b>Nao e "o que a busca encontrou".</b> A ferramenta pode devolver oito produtos e
         * a resposta mencionar dois; devolver os oito faria a tela pendurar cartao de coisa que
         * o assistente nunca recomendou. Aqui viajam so os que ele escreveu.
         *
         * <p><b>Por que o backend, e nao a tela.</b> A tela adivinhava: cruzava o texto da
         * resposta com o catalogo INTEIRO. Aqui o conjunto de candidatos e o que a ferramenta
         * devolveu nesta pergunta -- sabemos que o assistente viu aqueles produtos, entao
         * reconhecer o nome deles no texto e verificacao, nao palpite. Ver D-76 e D-92.
         */
        List<ProdutoResponse> produtosRecomendados
) {
    public static ChatMensagemResponse de(ChatMensagem mensagem) {
        return de(mensagem, List.of());
    }

    public static ChatMensagemResponse de(ChatMensagem mensagem, List<ProdutoResponse> citados) {
        return new ChatMensagemResponse(
                mensagem.getId(),
                mensagem.getSessaoId(),
                mensagem.getRemetente(),
                mensagem.getConteudo(),
                mensagem.getEnviadoEm(),
                citados
        );
    }
}
