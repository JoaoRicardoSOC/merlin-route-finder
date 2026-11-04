package br.com.jence.backend.factory;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class MapperFactoryTest {

    @Test
    @DisplayName("devolve sempre a mesma instancia")
    void instanciaUnica() {
        assertNotNull(MapperFactory.getMapper());
        assertSame(MapperFactory.getMapper(), MapperFactory.getMapper());
    }
}
