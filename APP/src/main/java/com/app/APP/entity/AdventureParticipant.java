package com.app.APP.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
        name = "participantes_aventura",
        uniqueConstraints = @UniqueConstraint(columnNames = {"aventura_id", "usuario_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdventureParticipant {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aventura_id", nullable = false)
    private Adventure adventure;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Column(name = "entrado_em")
    private LocalDateTime joinedAt;
}
