package com.trail.Cadastro.entity;

import com.trail.Cadastro.model.enums.RegistrationStatus;
import com.trail.Cadastro.trace.TraceContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

import static java.util.Objects.isNull;

@Entity
@Table(name = "usuario",
        uniqueConstraints = @UniqueConstraint(name = "uk_usuario_codigo", columnNames = "codigo_usuario"),
        indexes = @Index(name = "idx_usuario_email", columnList = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    private @Id String id;

    @Column(name = "nome")
    private String name;

    private String email;

    @Column(name = "senha")
    private String password;

    @Column(name = "codigo_usuario")
    private String userCode;

    private @Builder.Default RegistrationStatus status = RegistrationStatus.PENDENTE;

    @Column(name = "data_criacao")
    private LocalDateTime createdAt;

    @Column(name = "data_atualizacao")
    private LocalDateTime updatedAt;

    @Column(name = "trace_id")
    private String traceId;

    @PrePersist
    void onCreate() {
        if (isNull(traceId)) {
            traceId = TraceContext.current();
        }
    }
}
