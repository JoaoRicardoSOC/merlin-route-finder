package br.com.jence.backend.config;

import java.util.Map;

/**
 * Configuracao da aplicacao, lida das variaveis de ambiente.
 * <p>
 * Substitui o {@code application.yml} e o {@code @Value} do Spring: cada metodo le uma variavel
 * e, se ela nao existir, usa o mesmo valor padrao que o {@code application.yml} usava. Os nomes
 * das variaveis sao os mesmos de antes, para que o painel do Render nao precise mudar.
 * <p>
 * As credenciais nunca tem valor padrao, e por isso nunca aparecem no codigo - o contrario do
 * {@code ConnectionFactory} do material, que as fixa em constantes.
 */
public class Configuracao {

    private final Map<String, String> variaveis;

    /** Configuracao recebida pronta. Usado nos testes, para nao depender do ambiente da maquina. */
    public Configuracao(Map<String, String> variaveis) {
        this.variaveis = variaveis;
    }

    /** Configuracao lida das variaveis de ambiente do processo. */
    public static Configuracao doAmbiente() {
        return new Configuracao(System.getenv());
    }

    // ------------------------------------------------------------------ banco de dados

    public String getDbUrl() {
        return obrigatoria("DB_URL");
    }

    public String getDbUsuario() {
        return obrigatoria("DB_USER");
    }

    public String getDbSenha() {
        return obrigatoria("DB_PASSWORD");
    }

    // ------------------------------------------------------------------ servidor HTTP

    /** Provedores de hospedagem injetam a porta em PORT; sem respeitar isso, o deploy nao sobe. */
    public int getPorta() {
        return inteiro("PORT", 8080);
    }

    // ------------------------------------------------------------------ assistente de IA

    public String getIaUrlBase() {
        return texto("MERLIN_IA_BASE_URL", "https://generativelanguage.googleapis.com");
    }

    /**
     * Modelo "lite" escolhido com medicao, nao por ser o mais capaz: responde em cerca de 1s,
     * contra mais de 20s dos modelos que raciocinam antes de responder (D-37).
     */
    public String getIaModelo() {
        return texto("GEMINI_MODEL", "gemini-3.5-flash-lite");
    }

    /** Vazia quando nao configurada: o cliente de IA falha explicitamente e quem chama cai no fallback (D-35). */
    public String getIaChave() {
        return texto("GEMINI_API_KEY", "");
    }

    public long getHttpTempoLimiteConexaoSegundos() {
        return inteiro("MERLIN_HTTP_CONNECT_TIMEOUT_SECONDS", 5);
    }

    public long getHttpTempoLimiteLeituraSegundos() {
        return inteiro("MERLIN_HTTP_READ_TIMEOUT_SECONDS", 20);
    }

    // ------------------------------------------------------------------ tarefas de fundo

    public boolean isCargaDeDadosLigada() {
        return logico("MERLIN_SEED_ENABLED", true);
    }

    public boolean isAgendadorLigado() {
        return logico("MERLIN_AGENDADOR_ENABLED", true);
    }

    /** Meia hora: com o TTL de 4 horas, varrer mais vezes so gastaria consultas (D-42). */
    public long getAgendadorIntervaloMs() {
        return inteiro("AGENDADOR_INTERVALO_MS", 1_800_000);
    }

    /** O atraso inicial evita disputar o banco com a carga de dados na subida. */
    public long getAgendadorAtrasoInicialMs() {
        return inteiro("AGENDADOR_ATRASO_INICIAL_MS", 60_000);
    }

    // ------------------------------------------------------------------ leitura

    private String texto(String nome, String padrao) {
        String valor = variaveis.get(nome);
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    private String obrigatoria(String nome) {
        String valor = texto(nome, null);
        if (valor == null) {
            throw new IllegalStateException("Variavel de ambiente " + nome + " nao definida.");
        }
        return valor;
    }

    private int inteiro(String nome, int padrao) {
        String valor = texto(nome, null);
        if (valor == null) {
            return padrao;
        }
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Variavel de ambiente " + nome + " deveria ser um numero inteiro: '" + valor + "'.");
        }
    }

    private boolean logico(String nome, boolean padrao) {
        String valor = texto(nome, null);
        return valor == null ? padrao : Boolean.parseBoolean(valor);
    }
}
