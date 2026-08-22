package br.com.jence.backend.domain.service;

import java.util.List;

/**
 * Uma consulta que o assistente pode pedir para executar antes de responder.
 * <p>
 * E o mecanismo que impede alucinacao: o assistente nao inventa produtos porque so consegue
 * falar sobre o que a ferramenta devolveu (ver D-20).
 */
public record FerramentaIA(String nome, String descricao, List<ParametroIA> parametros) {

    /** Todo parametro e texto: e o suficiente para as consultas que o sistema expoe. */
    public record ParametroIA(String nome, String descricao, boolean obrigatorio) {

        public static ParametroIA obrigatorio(String nome, String descricao) {
            return new ParametroIA(nome, descricao, true);
        }

        public static ParametroIA opcional(String nome, String descricao) {
            return new ParametroIA(nome, descricao, false);
        }
    }
}
