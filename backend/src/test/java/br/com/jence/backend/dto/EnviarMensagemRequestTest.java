package br.com.jence.backend.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A validacao manual do corpo do chat, que substitui o @NotBlank e o @Size. */
class EnviarMensagemRequestTest {

    private List<StandardError.ValidationError> validar(String conteudo) {
        EnviarMensagemRequest requisicao = new EnviarMensagemRequest();
        requisicao.setConteudo(conteudo);
        return requisicao.validar();
    }

    @Test
    @DisplayName("conteudo ausente, vazio ou so com espacos e recusado")
    void conteudoEmBranco() {
        for (String conteudo : new String[] {null, "", "   "}) {
            List<StandardError.ValidationError> erros = validar(conteudo);

            assertEquals(1, erros.size(), "conteudo: [" + conteudo + "]");
            assertEquals("conteudo", erros.get(0).getField());
            assertEquals("conteudo e obrigatorio", erros.get(0).getMessage());
        }
    }

    @Test
    @DisplayName("1000 caracteres passam; 1001 nao")
    void limiteDeTamanho() {
        assertTrue(validar("a".repeat(1000)).isEmpty());

        List<StandardError.ValidationError> erros = validar("a".repeat(1001));
        assertEquals(1, erros.size());
        assertEquals("conteudo deve ter no maximo 1000 caracteres", erros.get(0).getMessage());
    }
}
