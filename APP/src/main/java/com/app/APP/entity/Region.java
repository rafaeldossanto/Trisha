package com.app.APP.entity;

import com.app.APP.model.enums.RegionVisibility;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regiao = "pasta" de aventuras do usuario. Tem dono, visibilidade propria e
 * uma lista de cidades que a compoem.
 */
@Entity
@Table(name = "regioes", indexes = {
        @Index(name = "idx_regiao_usuario", columnList = "usuario_id")
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Region {

    @Id
    private String id;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Column(name = "nome", nullable = false)
    private String name;

    @Column(name = "descricao")
    private String description;

    /** URL da foto de capa (upload via servico de midia). Opcional. Presigned
     *  URL carrega assinatura na query e estoura varchar(255) — dai o length. */
    @Column(name = "capa_url", length = 1024)
    private String coverUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibilidade", nullable = false)
    @Builder.Default
    private RegionVisibility visibility = RegionVisibility.PRIVADA;

    @ElementCollection
    @CollectionTable(name = "regiao_cidades", joinColumns = @JoinColumn(name = "regiao_id"))
    @Builder.Default
    private List<City> cities = new ArrayList<>();

    @CreatedDate
    @Column(name = "criado_em", updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private LocalDateTime updatedAt;
}
