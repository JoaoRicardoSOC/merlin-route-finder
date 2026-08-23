package br.com.jence.backend.application.usecase;

import br.com.jence.backend.domain.entity.Sessao;
import br.com.jence.backend.domain.repository.ListaRoteiroRepository;
import br.com.jence.backend.domain.repository.SessaoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Encerra as sessoes que ficaram para tras: o cliente parou de interagir e o TTL venceu.
 * <p>
 * Nao existe para proteger regra de negocio - {@code Sessao.isValida()} ja compara com o
 * relogio, entao uma sessao vencida e recusada mesmo que ninguem a tenha varrido. Existe para
 * que o banco reflita a realidade: sem isso, sessoes mortas ficariam {@code ACTIVE} para
 * sempre, e qualquer contagem de jornadas em andamento seria ficcao.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExpirarSessoesInativasUseCase {

    private final SessaoRepository sessaoRepository;
    private final ListaRoteiroRepository listaRoteiroRepository;

    /** O que a varredura fez, para o agendador registrar e o teste verificar. */
    public record ResultadoDaVarredura(int abandonadas, int expiradas, int falhas) {

        public int total() {
            return abandonadas + expiradas;
        }

        public boolean vazio() {
            return total() == 0 && falhas == 0;
        }
    }

    public ResultadoDaVarredura executar() {
        return executar(LocalDateTime.now());
    }

    /*
     * A referencia de tempo e parametro para o teste conseguir varrer um instante especifico
     * sem manipular o relogio do sistema. Mesmo motivo de Sessao.isValida(referencia).
     */
    public ResultadoDaVarredura executar(LocalDateTime referencia) {
        List<Sessao> vencidas = sessaoRepository.buscarExpiradas(referencia);

        int abandonadas = 0;
        int expiradas = 0;
        int falhas = 0;

        for (Sessao sessao : vencidas) {
            /*
             * Cada sessao e tratada isoladamente: uma que falhe - lista corrompida, indisponi-
             * bilidade momentanea do banco - nao pode impedir a varredura das demais. O salvar
             * do adaptador ja abre a propria transacao, entao nao ha o que desfazer em bloco.
             */
            try {
                if (tinhaCarrinhoMontado(sessao)) {
                    sessao.abandonar();
                    abandonadas++;
                } else {
                    sessao.expirar();
                    expiradas++;
                }
                sessaoRepository.salvar(sessao);

            } catch (Exception e) {
                falhas++;
                log.warn("Falha ao encerrar a sessao {} na varredura: {}", sessao.getId(), e.toString());
            }
        }

        ResultadoDaVarredura resultado = new ResultadoDaVarredura(abandonadas, expiradas, falhas);

        if (!resultado.vazio()) {
            log.info("Varredura de TTL: {} abandonada(s), {} expirada(s), {} falha(s)",
                    abandonadas, expiradas, falhas);
        }

        return resultado;
    }

    /*
     * A distincao entre os dois estados finais nao e enfeite, e a metrica que o produto promete
     * melhorar. ABANDONED e o carrinho abandonado no sentido classico do varejo: o cliente
     * montou a lista e nao concluiu - uma venda que quase aconteceu. EXPIRED e quem encostou
     * no totem e foi embora sem adicionar nada, onde nada estava em jogo. Ver D-42.
     */
    private boolean tinhaCarrinhoMontado(Sessao sessao) {
        return listaRoteiroRepository.buscarPorSessao(sessao.getId())
                .filter(lista -> !lista.isVazia())
                .isPresent();
    }
}
