package com.app.APP.entity;

import com.app.APP.model.enums.FriendshipStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDateTime;

@Entity
@Table(
        name = "amizades",
        uniqueConstraints = @UniqueConstraint(columnNames = {"solicitante_id", "receptor_id"}),
        indexes = {
                @Index(name = "idx_amizade_solicitante", columnList = "solicitante_id"),
                @Index(name = "idx_amizade_receptor", columnList = "receptor_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Friendship {

    @Id
    private String id;

    @Column(name = "solicitante_id", nullable = false)
    private String requesterId;

    @Column(name = "receptor_id", nullable = false)
    private String receiverId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private FriendshipStatus status = FriendshipStatus.PENDENTE;

    @Column(name = "solicitado_em")
    private LocalDateTime requestedAt;

    @Column(name = "respondido_em")
    private LocalDateTime respondedAt;

    /** Quem efetuou o bloqueio (preenchido apenas quando status = BLOQUEADA). */
    @Column(name = "bloqueado_por")
    private String blockedBy;
}
