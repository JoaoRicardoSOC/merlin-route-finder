package br.com.jence.backend.resource;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbException;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

/**
 * Converte o que chega como texto na requisicao - o id no caminho e o corpo JSON - dentro do
 * {@code try} do Resource, para que um valor malformado responda 400, e nao 404 ou 500.
 * <p>
 * Por que nao deixar o Jersey converter: com {@code UUID} no {@code @PathParam}, um id
 * malformado responde 404, como se o endereco nao existisse (RF-06). Com o DTO como parametro,
 * um JSON malformado responde 500 com uma pagina HTML do Grizzly (RF-07). Recebendo o texto e
 * convertendo aqui, os dois casos voltam a ser o 400 com {@code StandardError} de antes.
 */
public final class LeituraDaRequisicao {

    private static final Jsonb JSONB = JsonbBuilder.create();

    private LeituraDaRequisicao() {
    }

    /** O valor do caminho como UUID; se nao for um, o 400 de parametro invalido. */
    public static UUID uuid(String nome, String valor, String path) throws RequisicaoInvalida {
        try {
            return UUID.fromString(valor);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new RequisicaoInvalida(RespostaDeErro.parametroInvalido(nome, valor, path));
        }
    }

    /**
     * O valor da query como inteiro, ou {@code null} se ausente ou vazio; se nao for um
     * numero, o 400 de parametro invalido. Recebido como texto pelo mesmo motivo do UUID: com
     * {@code Integer} no {@code @QueryParam}, o Jersey responderia 404.
     */
    public static Integer inteiro(String nome, String valor, String path) throws RequisicaoInvalida {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            throw new RequisicaoInvalida(RespostaDeErro.parametroInvalido(nome, valor, path));
        }
    }

    /**
     * O valor da query como logico, ou {@code null} se ausente ou vazio. Aceita as mesmas
     * grafias que o Spring aceitava (true/false, on/off, yes/no, 1/0, sem diferenciar
     * maiusculas), para nenhum link que funcionava antes passar a dar 400.
     */
    public static Boolean logico(String nome, String valor, String path) throws RequisicaoInvalida {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        switch (valor.trim().toLowerCase()) {
            case "true", "on", "yes", "1":
                return Boolean.TRUE;
            case "false", "off", "no", "0":
                return Boolean.FALSE;
            default:
                throw new RequisicaoInvalida(RespostaDeErro.parametroInvalido(nome, valor, path));
        }
    }

    /** O corpo JSON como o DTO pedido; se nao der para ler, o 400 de corpo ilegivel. */
    public static <T> T corpo(String json, Class<T> tipo, String path) throws RequisicaoInvalida {
        T lido = corpoOpcional(json, tipo, path);
        if (lido == null) {
            throw new RequisicaoInvalida(RespostaDeErro.corpoIlegivel(path));
        }
        return lido;
    }

    /** Como {@link #corpo}, mas aceita a requisicao sem corpo, devolvendo {@code null}. */
    public static <T> T corpoOpcional(String json, Class<T> tipo, String path) throws RequisicaoInvalida {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return JSONB.fromJson(json, tipo);
        } catch (JsonbException e) {
            throw new RequisicaoInvalida(RespostaDeErro.corpoIlegivel(path));
        }
    }

    /** A requisicao nao pode ser lida; leva pronta a resposta 400 que o Resource devolve. */
    public static class RequisicaoInvalida extends Exception {

        private final transient Response resposta;

        public RequisicaoInvalida(Response resposta) {
            super("Requisicao invalida: status " + resposta.getStatus());
            this.resposta = resposta;
        }

        public Response getResposta() {
            return resposta;
        }
    }
}
