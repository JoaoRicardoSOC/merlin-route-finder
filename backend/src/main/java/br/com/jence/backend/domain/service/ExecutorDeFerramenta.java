package br.com.jence.backend.domain.service;

import java.util.Map;

/**
 * Executa a ferramenta pedida pelo assistente e devolve o resultado que sera enviado de
 * volta a ele.
 * <p>
 * Quem implementa e o caso de uso, que conhece o negocio e sabe consultar o banco. O
 * assistente apenas fala o protocolo.
 */
@FunctionalInterface
public interface ExecutorDeFerramenta {

    /**
     * @param nome       ferramenta pedida
     * @param argumentos valores que o assistente escolheu para os parametros
     * @return dados a devolver ao assistente, serializados como JSON
     */
    Map<String, Object> executar(String nome, Map<String, Object> argumentos);
}
