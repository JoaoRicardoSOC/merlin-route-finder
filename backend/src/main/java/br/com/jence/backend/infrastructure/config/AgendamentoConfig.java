package br.com.jence.backend.infrastructure.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Liga o agendamento do Spring.
 * <p>
 * Numa classe propria, e nao na {@code BackendApplication}, para que a capacidade de agendar
 * fique visivel como uma escolha do projeto e possa ser retirada sem tocar no ponto de entrada.
 */
@Configuration
@EnableScheduling
public class AgendamentoConfig {
}
