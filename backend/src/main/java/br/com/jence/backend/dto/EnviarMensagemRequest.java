package br.com.jence.backend.dto;
import java.util.ArrayList;
import java.util.List;

/** Espelha o schema {@code EnviarMensagemRequest} do contrato OpenAPI. */
public class EnviarMensagemRequest {

    private String conteudo;

    public EnviarMensagemRequest() {
    }

    public String getConteudo() {
        return conteudo;
    }

    public void setConteudo(String conteudo) {
        this.conteudo = conteudo;
    }

    /**
     * Confere o corpo com as mesmas regras e mensagens que as anotacoes de validacao tinham.
     *
     * @return os campos com problema; vazia quando o corpo esta certo
     */
    public List<StandardError.ValidationError> validar() {
        List<StandardError.ValidationError> erros = new ArrayList<>();
        if (conteudo == null || conteudo.isBlank()) {
            erros.add(StandardError.ValidationError.de("conteudo", "conteudo e obrigatorio"));
        }
        if (conteudo != null && conteudo.length() > 1000) {
            erros.add(StandardError.ValidationError.de("conteudo", "conteudo deve ter no maximo 1000 caracteres"));
        }
        return erros;
    }
}
