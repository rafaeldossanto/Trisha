package com.app.APP.entity;

import com.app.APP.model.enums.AdventureStatus;
import com.app.APP.model.enums.AdventureVisibility;
import com.app.APP.trace.TraceContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "aventuras", indexes = {
        @Index(name = "idx_aventura_usuario", columnList = "usuario_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Adventure {

    @Id
    private String id;

    @Column(name = "usuario_id", nullable = false)
    private String userId; // criador

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "regiao_id")
    private Region region;

    @Column(name = "destino", nullable = false)
    private String destination;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AdventureStatus status = AdventureStatus.PLANEJADA;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibilidade", nullable = false)
    @Builder.Default
    private AdventureVisibility visibility = AdventureVisibility.PRIVADA;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime updatedAt;

    @Column(name = "trace_id")
    private String traceId;

    @PrePersist
    void onCreate() {
        if (traceId == null) {
            traceId = TraceContext.current();
        }
    }
}
