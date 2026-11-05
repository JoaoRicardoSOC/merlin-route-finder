package br.com.jence.backend;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Navegacao no JSON das respostas, para os testes de integracao que chamam a API por HTTP.
 * <p>
 * Usa o JSON-P ({@code jakarta.json}), que vem junto do JSON-B, e imita os poucos metodos do
 * {@code JsonNode} do Jackson que esses testes usavam - o Jackson saiu junto com o Spring.
 * Como no Jackson, {@code get} de um campo ausente devolve {@code null}.
 */
public final class NoJson implements Iterable<NoJson> {

    private final JsonValue valor;

    private NoJson(JsonValue valor) {
        this.valor = valor;
    }

    public static NoJson ler(String texto) {
        try (JsonReader leitor = Json.createReader(new StringReader(texto))) {
            return new NoJson(leitor.readValue());
        }
    }

    public NoJson get(String campo) {
        if (!(valor instanceof JsonObject objeto) || !objeto.containsKey(campo)) {
            return null;
        }
        return new NoJson(objeto.get(campo));
    }

    public NoJson get(int indice) {
        if (!(valor instanceof JsonArray lista) || indice >= lista.size()) {
            return null;
        }
        return new NoJson(lista.get(indice));
    }

    public boolean has(String campo) {
        return valor instanceof JsonObject objeto && objeto.containsKey(campo);
    }

    public String asText() {
        return switch (valor.getValueType()) {
            case STRING -> ((JsonString) valor).getString();
            case NUMBER, TRUE, FALSE, NULL -> valor.toString();
            default -> "";
        };
    }

    public int asInt() {
        return valor instanceof JsonNumber numero ? numero.intValue() : Integer.parseInt(asText());
    }

    public long asLong() {
        return valor instanceof JsonNumber numero ? numero.longValue() : Long.parseLong(asText());
    }

    public boolean asBoolean() {
        return valor.getValueType() == JsonValue.ValueType.TRUE;
    }

    public boolean isNull() {
        return valor.getValueType() == JsonValue.ValueType.NULL;
    }

    public boolean isArray() {
        return valor instanceof JsonArray;
    }

    public int size() {
        if (valor instanceof JsonArray lista) {
            return lista.size();
        }
        if (valor instanceof JsonObject objeto) {
            return objeto.size();
        }
        return 0;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public Iterator<String> fieldNames() {
        return valor instanceof JsonObject objeto
                ? objeto.keySet().iterator()
                : List.<String>of().iterator();
    }

    /** Os elementos da lista, ou os valores do objeto, como no Jackson. */
    @Override
    public Iterator<NoJson> iterator() {
        List<NoJson> filhos = new ArrayList<>();
        if (valor instanceof JsonArray lista) {
            lista.forEach(item -> filhos.add(new NoJson(item)));
        } else if (valor instanceof JsonObject objeto) {
            objeto.values().forEach(item -> filhos.add(new NoJson(item)));
        }
        return filhos.iterator();
    }

    @Override
    public String toString() {
        return valor.toString();
    }
}
