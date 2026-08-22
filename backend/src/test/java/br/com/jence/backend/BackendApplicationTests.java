package br.com.jence.backend;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Sobe o contexto inteiro, incluindo a conexao com o Oracle, entao exige DB_URL, DB_USER e
 * DB_PASSWORD configurados. Marcado como integracao para nao deixar o build vermelho na
 * maquina de quem ainda nao tem credencial: rodar com {@code ./mvnw test -Pintegracao}.
 */
@Tag("integracao")
@SpringBootTest
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
