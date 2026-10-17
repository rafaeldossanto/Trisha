package com.app.APP.entity;

import com.app.APP.model.enums.PointType;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "pontos_interesse", indexes = {
        @Index(name = "idx_ponto_caminho", columnList = "caminho_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointOfInterest {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caminho_id", nullable = false)
    private Path path;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private PointType type;

    @Column(name = "nome")
    private String name;

    @Column(name = "descricao")
    private String description;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "trace_id")
    private String traceId;

    @PrePersist
    void onCreate() {
        if (traceId == null) {
            traceId = TraceContext.current();
        }
    }
}
