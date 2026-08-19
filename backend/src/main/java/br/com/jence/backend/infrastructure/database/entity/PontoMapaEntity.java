package br.com.jence.backend.infrastructure.database.entity;

import br.com.jence.backend.domain.entity.TipoPonto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "TB_PONTO_MAPA")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PontoMapaEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoPonto tipo;

    @Column(name = "corredor", length = 100)
    private String corredor;

    @Column(name = "coordenada_x", nullable = false)
    private int coordenadaX;

    @Column(name = "coordenada_y", nullable = false)
    private int coordenadaY;
}
