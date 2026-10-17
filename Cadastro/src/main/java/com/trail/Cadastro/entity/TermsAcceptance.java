package com.trail.Cadastro.entity;

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
public class TermsAcceptance {

    private @Id String id;

    @OneToOne
    @JoinColumn(name = "usuario_id")
    private User user;

    @Column(name = "aceito")
    private Boolean accepted;

    @Column(name = "versao")
    private String version;

    @Column(name = "data_aceite")
    private LocalDateTime acceptedAt;
}
