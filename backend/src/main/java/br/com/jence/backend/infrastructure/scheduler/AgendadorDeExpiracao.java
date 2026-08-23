package br.com.jence.backend.infrastructure.scheduler;

import br.com.jence.backend.application.usecase.ExpirarSessoesInativasUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "merlin.agendador.enabled", havingValue = "true", matchIfMissing = true)
public class AgendadorDeExpiracao {

    private final ExpirarSessoesInativasUseCase expirarSessoesInativasUseCase;

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
             * O agendador do Spring interrompe as execucoes seguintes de uma tarefa que lanca.
             * Como o motivo mais provavel de falha aqui e o banco estar momentaneamente fora,
             * deixar a excecao subir aposentaria a varredura ate o proximo restart.
             */
            log.error("Varredura de TTL falhou; a proxima segue agendada", e);
        }
    }
}
