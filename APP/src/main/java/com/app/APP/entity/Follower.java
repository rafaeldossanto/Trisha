package com.app.APP.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Relacao de "seguir" (direcional): followerId segue followedId. Diferente da
 * amizade (bidirecional, com aceite) — seguir nao exige confirmacao.
 */
@Entity
@Table(name = "seguidores",
        uniqueConstraints = @UniqueConstraint(name = "uk_seguidor", columnNames = {"seguidor_id", "seguido_id"}),
        indexes = {
                @Index(name = "idx_seguidor_seguidor", columnList = "seguidor_id"),
                @Index(name = "idx_seguidor_seguido", columnList = "seguido_id")
        })
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Follower {

    @Id
    private String id;

    @Column(name = "seguidor_id", nullable = false)
    private String followerId;

    @Column(name = "seguido_id", nullable = false)
    private String followedId;

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime createdAt;
}
