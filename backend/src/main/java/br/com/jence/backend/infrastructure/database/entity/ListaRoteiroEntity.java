package br.com.jence.backend.infrastructure.database.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "TB_LISTA_ROTEIRO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ListaRoteiroEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    /*
     * sessao_id e mapeado duas vezes de proposito:
     * - sessaoId e o campo gravavel, espelhando o dominio (que guarda apenas o UUID);
     * - sessao existe so para o Hibernate gerar a constraint de FK prevista no DER,
     *   por isso insertable/updatable = false.
     */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sessao_id", nullable = false, length = 36)
    private UUID sessaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", insertable = false, updatable = false)
    private SessaoEntity sessao;

    @OneToMany(mappedBy = "listaRoteiro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ItemRoteiroEntity> itens = new ArrayList<>();

    @Column(name = "handoff_token", length = 1000)
    private String handoffToken;

    @Column(name = "token_expiracao")
    private LocalDateTime tokenExpiracao;
}
