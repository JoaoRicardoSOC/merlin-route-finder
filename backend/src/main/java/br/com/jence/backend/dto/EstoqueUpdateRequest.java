package br.com.jence.backend.dto;
import java.util.ArrayList;
import java.util.List;

/**
 * Espelha o schema {@code EstoqueUpdateRequest} do contrato OpenAPI.
 * <p>
 * Um unico campo: o saldo passa a valer o que for enviado. Zero dispara o cenario de ruptura,
 * qualquer valor positivo restaura o produto.
 */
public class EstoqueUpdateRequest {

    private Integer saldoEstoque;

    public EstoqueUpdateRequest() {
    }

    public Integer getSaldoEstoque() {
        return saldoEstoque;
    }

    public void setSaldoEstoque(Integer saldoEstoque) {
        this.saldoEstoque = saldoEstoque;
    }

    /**
     * Confere o corpo com as mesmas regras e mensagens que as anotacoes de validacao tinham.
     *
     * @return os campos com problema; vazia quando o corpo esta certo
     */
    public List<StandardError.ValidationError> validar() {
        List<StandardError.ValidationError> erros = new ArrayList<>();
        if (saldoEstoque == null) {
            erros.add(StandardError.ValidationError.de("saldoEstoque", "saldoEstoque e obrigatorio"));
        }
        if (saldoEstoque != null && saldoEstoque < 0) {
            erros.add(StandardError.ValidationError.de("saldoEstoque", "saldoEstoque nao pode ser negativo"));
        }
        return erros;
    }
}
