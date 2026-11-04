package br.com.jence.backend.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfiguracaoTest {

    @Test
    @DisplayName("Sem variaveis definidas, vale o mesmo padrao que o application.yml usava")
    void padroes() {
        Configuracao configuracao = new Configuracao(Map.of());

        assertEquals(8080, configuracao.getPorta());
        assertEquals("https://generativelanguage.googleapis.com", configuracao.getIaUrlBase());
        assertEquals("gemini-3.5-flash-lite", configuracao.getIaModelo());
        assertEquals("", configuracao.getIaChave());
        assertEquals(5, configuracao.getHttpTempoLimiteConexaoSegundos());
        assertEquals(20, configuracao.getHttpTempoLimiteLeituraSegundos());
        assertTrue(configuracao.isCargaDeDadosLigada());
        assertTrue(configuracao.isAgendadorLigado());
        assertEquals(1_800_000, configuracao.getAgendadorIntervaloMs());
        assertEquals(60_000, configuracao.getAgendadorAtrasoInicialMs());
    }

    @Test
    @DisplayName("Variavel definida vence o padrao")
    void variavelDefinida() {
        Configuracao configuracao = new Configuracao(Map.of(
                "PORT", "10000",
                "GEMINI_MODEL", "outro-modelo",
                "MERLIN_AGENDADOR_ENABLED", "false",
                "AGENDADOR_INTERVALO_MS", "5000"));

        assertEquals(10000, configuracao.getPorta());
        assertEquals("outro-modelo", configuracao.getIaModelo());
        assertFalse(configuracao.isAgendadorLigado());
        assertEquals(5000, configuracao.getAgendadorIntervaloMs());
    }

    @Test
    @DisplayName("Variavel em branco conta como nao definida, como no painel do Render")
    void variavelEmBranco() {
        Configuracao configuracao = new Configuracao(Map.of("PORT", "  ", "GEMINI_API_KEY", ""));

        assertEquals(8080, configuracao.getPorta());
        assertEquals("", configuracao.getIaChave());
    }

    @Test
    @DisplayName("Credencial do banco nao tem padrao: sem ela, a falha diz qual variavel falta")
    void credencialObrigatoria() {
        Configuracao configuracao = new Configuracao(Map.of("DB_URL", "jdbc:oracle:thin:@host:1521:orcl"));

        assertEquals("jdbc:oracle:thin:@host:1521:orcl", configuracao.getDbUrl());
        IllegalStateException erro = assertThrows(IllegalStateException.class, configuracao::getDbSenha);
        assertTrue(erro.getMessage().contains("DB_PASSWORD"));
    }

    @Test
    @DisplayName("Numero invalido falha na subida, com o nome da variavel, em vez de cair no padrao")
    void numeroInvalido() {
        Configuracao configuracao = new Configuracao(Map.of("PORT", "oito mil"));

        IllegalStateException erro = assertThrows(IllegalStateException.class, configuracao::getPorta);
        assertTrue(erro.getMessage().contains("PORT"));
    }
}
