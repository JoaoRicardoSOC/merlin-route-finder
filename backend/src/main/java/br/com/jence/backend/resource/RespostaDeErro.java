package br.com.jence.backend.resource;

import br.com.jence.backend.dto.StandardError;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;

/**
 * As respostas de erro que os Resources devolvem nos seus {@code catch}, com o corpo
 * {@link StandardError}.
 * <p>
 * Os status e os textos sao os mesmos que o {@code GlobalExceptionHandler} do Spring devolvia,
 * para o frontend nao perceber a troca. Ficam aqui, e nao repetidos em cada Resource, para que
 * um texto corrigido valha para todos.
 */
public final class RespostaDeErro {

    private RespostaDeErro() {
    }

    /** 400: o corpo chegou legivel, mas com campos que nao passam na validacao. */
    public static Response validacao(String path, List<StandardError.ValidationError> erros) {
        return montar(StandardError.falhaDeValidacao(path, erros));
    }

    /**
     * 400: um valor do caminho ou da query nao e do tipo esperado, como um UUID malformado.
     * Erro de quem chamou, e nao do servidor - por isso nao pode virar 500.
     */
    public static Response parametroInvalido(String nome, String valor, String path) {
        return montar(StandardError.de(400, "Parâmetro Inválido",
                "O valor '%s' não é válido para o parâmetro '%s'.".formatted(valor, nome),
                path, null));
    }

    /** 400: o corpo nao e um JSON valido para o que o endpoint espera. */
    public static Response corpoIlegivel(String path) {
        return montar(StandardError.de(400, "Corpo da Requisição Inválido",
                "O corpo enviado não pode ser lido. Verifique se é um JSON válido.",
                path, null));
    }

    /**
     * 400: a query chegou com bytes que nao formam texto em UTF-8, como um "a" com acento
     * codificado em latin-1. Erro do pedido, e nao nosso (D-74).
     */
    public static Response textoIndecodificavel(String path) {
        return montar(StandardError.de(400, "Requisição Inválida",
                "Não foi possível ler os parâmetros enviados. Verifique se a requisição está "
                        + "codificada em UTF-8.",
                path, null));
    }

    /** 404: o recurso pedido nao existe. */
    public static Response recursoNaoEncontrado(Exception e, String path) {
        return montar(StandardError.de(404, "Recurso Não Encontrado", e.getMessage(), path, null));
    }

    /** 409: o recurso existe, mas o estado dele nao permite a operacao. */
    public static Response operacaoNaoPermitida(Exception e, String path) {
        return montar(StandardError.de(409, "Operação Não Permitida", e.getMessage(), path, null));
    }

    /**
     * 422, e nao 404: o item existe e a ruptura foi registrada - o que nao existe e um
     * substituto a oferecer. O celular precisa distinguir os dois casos.
     */
    public static Response substitutoIndisponivel(Exception e, String path) {
        return montar(StandardError.de(422, "Substituto Indisponível", e.getMessage(), path, null));
    }

    /**
     * 500: falha nossa ou da infraestrutura, como o banco fora do ar. A causa vai para o log;
     * o corpo leva uma mensagem generica, para nao expor detalhe interno a quem chamou.
     */
    public static Response erroInterno(Exception e, String path) {
        System.err.println("Erro inesperado em " + path);
        e.printStackTrace();
        return montar(StandardError.de(500, "Erro Interno do Servidor",
                "Ocorreu um erro inesperado. Tente novamente em instantes.", path, null));
    }

    private static Response montar(StandardError erro) {
        return Response.status(erro.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(erro)
                .build();
    }
}
