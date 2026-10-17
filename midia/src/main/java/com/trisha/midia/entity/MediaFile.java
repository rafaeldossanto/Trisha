package com.trisha.midia.entity;

import com.trisha.midia.model.enums.FileType;
import com.trisha.midia.trace.TraceContext;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "arquivo_midia")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaFile {

    @Id
    private String id;

    @Column(name = "nome_original", nullable = false)
    private String originalName;

    @Column(name = "nome_armazenado", nullable = false)
    private String storedName;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private FileType type;

    @Column(nullable = false)
    private String contentType;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long sizeBytes;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false)
    private String url;

    /** Quem fez o upload — so o dono pode apagar o arquivo. */
    @Column(name = "proprietario_id")
    private String ownerId;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "trace_id")
    private String traceId;

    @PrePersist
    void onCreate() {
        if (traceId == null) {
            traceId = TraceContext.current();
        }
    }
}
