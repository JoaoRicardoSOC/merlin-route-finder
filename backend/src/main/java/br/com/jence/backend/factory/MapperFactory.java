package br.com.jence.backend.factory;

import org.modelmapper.ModelMapper;

/**
 * Entrega o {@link ModelMapper} usado para copiar objetos de dominio para DTOs.
 * <p>
 * O material cria um {@code new ModelMapper()} a cada conversao. Aqui ele e criado uma vez so:
 * montar o mapeamento entre duas classes custa caro, e o {@code ModelMapper} guarda esse
 * mapeamento depois da primeira copia. A instancia pode ser usada por varias threads ao mesmo
 * tempo.
 */
public final class MapperFactory {

    private static final ModelMapper MAPPER = new ModelMapper();

    private MapperFactory() {
    }

    public static ModelMapper getMapper() {
        return MAPPER;
    }
}
