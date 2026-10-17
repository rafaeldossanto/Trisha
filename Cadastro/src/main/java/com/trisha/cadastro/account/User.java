package com.trisha.cadastro.account;

import com.trisha.cadastro.shared.trace.TraceContext;
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

    // --- transicoes de status (quem grava e o UserService) ------------------

    public boolean isActive() {
        return RegistrationStatus.ATIVO.equals(status);
    }

    public void activate(LocalDateTime now) {
        status = RegistrationStatus.ATIVO;
        updatedAt = now;
    }

    /**
     * Nao mexe no updatedAt de proposito: e o comportamento que a desativacao
     * sempre teve (rota DELETE e prazo de confirmacao estourado).
     */
    public void deactivate() {
        status = RegistrationStatus.INATIVO;
    }

    /**
     * Recadastro sobre uma conta INATIVA: reaproveita o registro (id, codigo e
     * data de criacao) e volta para PENDENTE. O updatedAt recomeca o prazo de
     * confirmacao, que o RegistrationCleanupJob cobra a partir dele.
     */
    public void reopen(String name, String encodedPassword) {
        this.name = name;
        this.password = encodedPassword;
        this.status = RegistrationStatus.PENDENTE;
        this.updatedAt = LocalDateTime.now();
    }
}
