package br.com.jence.backend.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Uma ruptura fisica reportada pelo cliente diante da prateleira (UC-013).
 * <p>
 * Nao existia no DER original. Foi acrescentada porque, sem ela, o alerta ajuda um cliente e
 * a loja nao aprende nada: o registro transforma o botao "Prateleira Vazia" num relato
 * continuo de divergencia entre o estoque do sistema e o que existe na gondola - que e
 * exatamente o risco de "ruptura silenciosa" levantado no inicio do projeto (ver D-23).
 * <p>
 * O relato e <b>evidencia, nao verdade</b>: ele nao altera o saldo do produto. Ver D-38.
 */
@Getter
public class RegistroRuptura {

    private final UUID id;
    private final UUID sessaoId;
    private final UUID itemRoteiroId;
    private final UUID produtoFaltanteId;
    private final UUID produtoSugeridoId;
    private final String justificativa;
    private final OrigemSugestao origem;
    private final LocalDateTime registradoEm;

    private RegistroRuptura(UUID id, UUID sessaoId, UUID itemRoteiroId, UUID produtoFaltanteId,
                            UUID produtoSugeridoId, String justificativa, OrigemSugestao origem,
                            LocalDateTime registradoEm) {
        this.id = id;
        this.sessaoId = sessaoId;
        this.itemRoteiroId = itemRoteiroId;
        this.produtoFaltanteId = produtoFaltanteId;
        this.produtoSugeridoId = produtoSugeridoId;
        this.justificativa = justificativa;
        this.origem = origem;
        this.registradoEm = registradoEm;
    }

    public static RegistroRuptura comSugestao(UUID sessaoId, UUID itemRoteiroId, UUID produtoFaltanteId,
                                              UUID produtoSugeridoId, String justificativa,
                                              OrigemSugestao origem) {
        return new RegistroRuptura(UUID.randomUUID(), sessaoId, itemRoteiroId, produtoFaltanteId,
                produtoSugeridoId, justificativa, origem, LocalDateTime.now());
    }

    /** A ruptura e registrada mesmo sem substituto: para a loja, saber que faltou ja e o dado. */
    public static RegistroRuptura semSugestao(UUID sessaoId, UUID itemRoteiroId, UUID produtoFaltanteId,
                                              String motivo) {
        return new RegistroRuptura(UUID.randomUUID(), sessaoId, itemRoteiroId, produtoFaltanteId,
                null, motivo, OrigemSugestao.NENHUMA, LocalDateTime.now());
    }

    public static RegistroRuptura reconstituir(UUID id, UUID sessaoId, UUID itemRoteiroId,
                                               UUID produtoFaltanteId, UUID produtoSugeridoId,
                                               String justificativa, OrigemSugestao origem,
                                               LocalDateTime registradoEm) {
        return new RegistroRuptura(id, sessaoId, itemRoteiroId, produtoFaltanteId, produtoSugeridoId,
                justificativa, origem, registradoEm);
    }

    public boolean temSugestao() {
        return produtoSugeridoId != null;
    }
}
