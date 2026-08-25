package br.com.jence.backend.infrastructure.database.entity;

import br.com.jence.backend.domain.entity.StatusSessao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "TB_SESSAO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SessaoEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusSessao status;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm;

    @Column(name = "expiracao_ttl", nullable = false)
    private LocalDateTime expiracaoTtl;

    /*
     * A placa lida. ManyToOne e nao um UUID solto porque o ponto e sempre carregado junto com
     * a sessao - toda resposta de sessao mostra onde o cliente esta.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "ponto_escaneado_id")
    private PontoMapaEntity pontoEscaneado;

    @Column(name = "escaneado_em")
    private LocalDateTime escaneadoEm;
}
