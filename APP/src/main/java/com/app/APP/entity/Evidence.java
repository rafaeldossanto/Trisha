package com.app.APP.entity;

import com.app.APP.model.enums.EvidenceType;
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
@Table(name = "evidencias", indexes = {
        @Index(name = "idx_evidencia_ponto", columnList = "ponto_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Evidence {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ponto_id", nullable = false)
    private PointOfInterest point;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Column(name = "foto_url", nullable = false)
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_evidencia")
    private EvidenceType evidenceType;

    @Column(name = "lat_captura")
    private Double captureLat;

    @Column(name = "lng_captura")
    private Double captureLng;

    @Column(name = "distancia_doponto_m")
    private Double distanceFromPointM;

    @Column(name = "capturada_no_app", nullable = false)
    @Builder.Default
    private Boolean capturedInApp = false;

    @Column(name = "validada", nullable = false)
    @Builder.Default
    private Boolean validated = false;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime createdAt;
}
