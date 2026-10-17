package com.trail.Cadastro.entity;

import com.trail.Cadastro.model.enums.ConfirmationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailConfirmation {

    private @Id String id;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private User user;
    private String token;
    private ConfirmationStatus status;

    @Column(name = "expira_em")
    private LocalDateTime expiresAt;

    @Column(name = "enviado_em")
    private LocalDateTime sentAt;

    @Column(name = "data_confirmacao")
    private LocalDateTime confirmedAt;
}
