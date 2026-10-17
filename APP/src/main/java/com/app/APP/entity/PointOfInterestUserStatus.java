package com.app.APP.entity;

import com.app.APP.model.enums.PointStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Marcacao pessoal do usuario sobre um ponto de interesse: o status de
 * progressao e a flag de objetivo, independente do status (um ponto pode ser
 * objetivo e depois ser conquistado). A linha so existe enquanto ha alguma
 * marcacao — zerou status e objetivo, a linha e removida.
 */
@Entity
@Table(name = "status_ponto_interesse",
        uniqueConstraints = @UniqueConstraint(name = "uk_status_ponto_usuario", columnNames = {"ponto_id", "usuario_id"}),
        indexes = {
                @Index(name = "idx_status_ponto_usuario", columnList = "usuario_id"),
                @Index(name = "idx_status_ponto_ponto", columnList = "ponto_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointOfInterestUserStatus {

    @Id
    private String id;

    @Column(name = "ponto_id", nullable = false)
    private String pointId;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private PointStatus status;

    @Column(name = "objetivo", nullable = false)
    private boolean goal;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime updatedAt;
}
