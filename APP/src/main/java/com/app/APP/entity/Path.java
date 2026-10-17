package com.app.APP.entity;

import com.app.APP.model.enums.Color;
import com.app.APP.trace.TraceContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

import static com.app.APP.model.enums.Color.ROXO;

@Entity
@Table(name = "caminhos", indexes = {
        @Index(name = "idx_caminho_aventura", columnList = "aventura_id"),
        @Index(name = "idx_caminho_usuario", columnList = "usuario_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Path {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aventura_id", nullable = false)
    private Adventure adventure;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Builder.Default
    @Column(name = "cor")
    private Color color = ROXO;

    @Column(name = "numero")
    private Integer number;

    @Column(name = "iniciado_em")
    private LocalDateTime startedAt;

    @Column(name = "finalizado_em")
    private LocalDateTime finishedAt;

    @Column(name = "distancia_total_km")
    private Double totalDistanceKm;

    @Column(name = "trace_id")
    private String traceId;

    @PrePersist
    void onCreate() {
        if (traceId == null) {
            traceId = TraceContext.current();
        }
    }
}
