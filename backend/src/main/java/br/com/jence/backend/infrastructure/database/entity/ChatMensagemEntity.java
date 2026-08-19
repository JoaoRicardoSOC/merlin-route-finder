package br.com.jence.backend.infrastructure.database.entity;

import br.com.jence.backend.domain.entity.Remetente;
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
@Table(name = "TB_CHAT_MENSAGEM")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMensagemEntity {

    @Id
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "id", length = 36)
    private UUID id;

    /* Mesmo padrao de ListaRoteiroEntity: sessaoId grava, sessao existe so para gerar a FK. */
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "sessao_id", nullable = false, length = 36)
    private UUID sessaoId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sessao_id", insertable = false, updatable = false)
    private SessaoEntity sessao;

    @Enumerated(EnumType.STRING)
    @Column(name = "remetente", nullable = false, length = 20)
    private Remetente remetente;

    @Lob
    @Column(name = "conteudo", nullable = false)
    private String conteudo;

    @Column(name = "enviado_em", nullable = false)
    private LocalDateTime enviadoEm;
}
