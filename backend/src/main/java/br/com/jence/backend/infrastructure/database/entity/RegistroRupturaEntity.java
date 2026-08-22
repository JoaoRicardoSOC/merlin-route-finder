package br.com.jence.backend.infrastructure.database.entity;

import br.com.jence.backend.domain.entity.OrigemSugestao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "TB_REGISTRO_RUPTURA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegistroRupturaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    /* Mesmo padrao de ChatMensagemEntity: sessaoId grava, sessao existe so para gerar a FK. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sessao_id", nullable = false, length = 36)
    private UUID sessaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", insertable = false, updatable = false)
    private SessaoEntity sessao;

    /*
     * Guardado como valor solto, sem FK para TB_ITEM_ROTEIRO. O item pode ser removido do
     * carrinho pelo cliente depois de relatar a ruptura, e o registro precisa sobreviver a
     * isso: para a loja, a informacao de que a gondola estava vazia continua valendo.
     */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "item_roteiro_id", nullable = false, length = 36)
    private UUID itemRoteiroId;

    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "produto_faltante_id", nullable = false, length = 36)
    private UUID produtoFaltanteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_faltante_id", insertable = false, updatable = false)
    private ProdutoEntity produtoFaltante;

    /* Nulo quando nenhum substituto plausivel foi encontrado. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "produto_sugerido_id", length = 36)
    private UUID produtoSugeridoId;

    @Lob
    @Column(name = "justificativa")
    private String justificativa;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem", nullable = false, length = 20)
    private OrigemSugestao origem;

    @Column(name = "registrado_em", nullable = false)
    private LocalDateTime registradoEm;
}
