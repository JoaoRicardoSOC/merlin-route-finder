package br.com.jence.backend.infrastructure.database.entity;

import br.com.jence.backend.domain.entity.AtributoProduto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * Uma caracteristica de um produto.
 * <p>
 * Tabela separada, e nao colunas em {@code TB_PRODUTO}, porque os atributos <b>nao sao os
 * mesmos para todo produto</b>: amperagem so existe em disjuntor, grao so em lixa, rendimento
 * so em tinta. Como colunas, a tabela ficaria larga e quase toda nula. Ver D-62.
 */
@Entity
@Table(name = "TB_PRODUTO_ATRIBUTO",
        uniqueConstraints = @UniqueConstraint(
                name = "UK_PRODUTO_ATRIBUTO", columnNames = {"produto_id", "chave"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoAtributoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "produto_id", nullable = false)
    private ProdutoEntity produto;

    @Enumerated(EnumType.STRING)
    @Column(name = "chave", nullable = false, length = 30)
    private AtributoProduto chave;

    @Column(name = "valor", nullable = false, length = 100)
    private String valor;
}
