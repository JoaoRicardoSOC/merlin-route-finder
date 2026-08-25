package br.com.jence.backend.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
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
@Table(name = "TB_ITEM_ROTEIRO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemRoteiroEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lista_id", nullable = false)
    private ListaRoteiroEntity listaRoteiro;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private ProdutoEntity produto;

    /*
     * Redundante com coletado_em de proposito. A coluna nasceu NOT NULL e sem default, e o
     * ddl-auto: update nunca remove nada (D-51): parar de grava-la faria todo insert de item
     * falhar nos bancos que ja existem. O dominio continua sendo a fonte unica - as duas
     * colunas saem do mesmo ItemRoteiro.
     */
    @Column(name = "coletado", nullable = false)
    private boolean coletado;

    @Column(name = "coletado_em")
    private LocalDateTime coletadoEm;
}
