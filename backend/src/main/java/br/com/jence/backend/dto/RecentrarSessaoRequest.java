package br.com.jence.backend.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Corpo de {@code PUT /sessoes/{sessaoId}/posicao}: a placa onde o cliente esta agora.
 * <p>
 * Formato igual ao de {@link IniciarSessaoRequest}, significado diferente: la o codigo e
 * opcional, porque a sessao pode nascer sem posicao; aqui e o proprio objeto da operacao, e
 * uma requisicao sem ele nao quer dizer nada.
 */
public class RecentrarSessaoRequest {

    /** Codigo de localizacao impresso na placa, em qualquer grafia. */
    private String codigoPonto;

    public RecentrarSessaoRequest() {
    }

    public String getCodigoPonto() {
        return codigoPonto;
    }

    public void setCodigoPonto(String codigoPonto) {
        this.codigoPonto = codigoPonto;
    }

    /**
     * Confere o corpo com as mesmas regras e mensagens que as anotacoes de validacao tinham.
     *
     * @return os campos com problema; vazia quando o corpo esta certo
     */
    public List<StandardError.ValidationError> validar() {
        List<StandardError.ValidationError> erros = new ArrayList<>();
        if (codigoPonto == null || codigoPonto.isBlank()) {
            erros.add(StandardError.ValidationError.de("codigoPonto", "informe o codigo da placa"));
        }
        return erros;
    }
}
