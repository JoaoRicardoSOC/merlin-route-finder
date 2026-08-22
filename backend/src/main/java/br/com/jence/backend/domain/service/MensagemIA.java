package br.com.jence.backend.domain.service;

/** Uma fala da conversa com o assistente. */
public record MensagemIA(PapelIA papel, String conteudo) {

    public static MensagemIA doCliente(String conteudo) {
        return new MensagemIA(PapelIA.CLIENTE, conteudo);
    }

    public static MensagemIA doAssistente(String conteudo) {
        return new MensagemIA(PapelIA.ASSISTENTE, conteudo);
    }
}
