package br.com.jence.backend.scheduler;

import br.com.jence.backend.service.ExpirarSessoesInativasUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Dispara a varredura de sessoes vencidas em intervalo fixo.
 * <p>
 * So dispara: a regra de o que fazer com cada sessao vive no caso de uso, que assim pode ser
 * testado sem envolver agendamento nenhum. Mesma separacao entre porta e adaptador usada no
 * resto do projeto - aqui o "adaptador" e o relogio.
 * <p>
 * Pode ser desligado com {@code merlin.agendador.enabled=false}, mesmo padrao do
 * {@code CarregadorDadosIniciais}.
 */
@Component
@ConditionalOnProperty(name = "merlin.agendador.enabled", havingValue = "true", matchIfMissing = true)
public class AgendadorDeExpiracao {

    private final ExpirarSessoesInativasUseCase expirarSessoesInativasUseCase;

    /** Uma thread so: duas varreduras ao mesmo tempo disputariam as mesmas sessoes. */
    private ScheduledExecutorService executor;

    public AgendadorDeExpiracao(ExpirarSessoesInativasUseCase expirarSessoesInativasUseCase) {
        this.expirarSessoesInativasUseCase = expirarSessoesInativasUseCase;
    }

    /**
     * Comeca a varrer. Chamado pela Main antes de abrir o servidor.
     * <p>
     * {@code scheduleWithFixedDelay}, e nao {@code scheduleAtFixedRate}: o intervalo conta a
     * partir do fim da varredura anterior, entao uma passagem lenta nunca se sobrepoe a seguinte.
     * O atraso inicial evita competir com a carga de dados no startup.
     */
    public synchronized void iniciar(long atrasoInicialMs, long intervaloMs) {
        if (executor != null) {
            return;
        }
        executor = Executors.newSingleThreadScheduledExecutor(tarefa -> {
            Thread thread = new Thread(tarefa, "agendador-de-expiracao");
            // Nao segura a JVM: quem decide quando a aplicacao termina e a Main.
            thread.setDaemon(true);
            return thread;
        });
        executor.scheduleWithFixedDelay(this::varrerSessoesVencidas,
                atrasoInicialMs, intervaloMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Para de varrer. Chamado no desligamento da aplicacao. Espera alguns segundos pela
     * varredura em andamento, para ela nao ser cortada no meio de uma transacao.
     */
    public synchronized void parar() {
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            executor.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor = null;
    }

    /*
     * fixedDelayString e nao fixedRate: o intervalo conta a partir do fim da execucao anterior.
     * Com fixedRate, uma varredura lenta comecaria a se sobrepor a seguinte, e duas passagens
     * simultaneas disputariam as mesmas sessoes.
     *
     * O atraso inicial evita competir com a carga de dados no startup.
     */
    @Scheduled(fixedDelayString = "${merlin.agendador.intervalo-ms:300000}",
            initialDelayString = "${merlin.agendador.atraso-inicial-ms:60000}")
    public void varrerSessoesVencidas() {
        try {
            expirarSessoesInativasUseCase.executar();

        } catch (Exception e) {
            /*
             * O agendador (o do Spring e o ScheduledExecutorService) interrompe as execucoes
             * seguintes de uma tarefa que lanca.
             * Como o motivo mais provavel de falha aqui e o banco estar momentaneamente fora,
             * deixar a excecao subir aposentaria a varredura ate o proximo restart.
             */
            System.err.println("Varredura de TTL falhou; a proxima segue agendada");
            e.printStackTrace();
        }
    }
}
