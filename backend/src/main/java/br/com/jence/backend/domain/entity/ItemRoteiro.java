package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class ItemRoteiro {

    private final UUID id;
    private final Produto produto;

    /**
     * Quando o cliente pegou este item da prateleira, ou {@code null} se ainda nao pegou.
     * <p>
     * E um instante, e nao um booleano, porque a posicao do cliente vem do <b>ultimo</b> item
     * coletado - e "ultimo" precisa de ordem. Ate a Fase 3 essa ordem vinha do campo de rota;
     * sem rota, o momento da coleta e o unico registro do que aconteceu primeiro.
     */
    private LocalDateTime coletadoEm;

    public ItemRoteiro(UUID id, Produto produto) {
        this(id, produto, null);
    }

    private ItemRoteiro(UUID id, Produto produto, LocalDateTime coletadoEm) {
        this.id = id;
        this.produto = produto;
        this.coletadoEm = coletadoEm;
    }

    public static ItemRoteiro reconstituir(UUID id, Produto produto, LocalDateTime coletadoEm) {
        return new ItemRoteiro(id, produto, coletadoEm);
    }

    /** O que o contrato expoe: o cliente so precisa saber se ja pegou, nao quando. */
    public boolean isColetado() {
        return coletadoEm != null;
    }

    /**
     * Idempotente por escolha: tocar duas vezes, ou a rede reenviar, nao pode mover a posicao
     * do cliente para tras nem para frente. Vale a hora da primeira confirmacao.
     */
    public void marcarComoColetado(LocalDateTime quando) {
        if (coletadoEm == null) {
            this.coletadoEm = quando;
        }
    }
}
