package com.app.APP.entity;

import com.app.APP.model.enums.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "midias", indexes = {
        @Index(name = "idx_midia_aventura", columnList = "aventura_id"),
        @Index(name = "idx_midia_caminho", columnList = "caminho_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Media {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aventura_id", nullable = false)
    private Adventure adventure;

    // nullable — se nulo, midia é avulsa na aventura
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "caminho_id")
    private Path path;

    @Column(name = "usuario_id", nullable = false)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private MediaType type;

    @Column(nullable = false)
    private String url;

    @Column(name = "lat_captura")
    private Double captureLat;

    @Column(name = "lng_captura")
    private Double captureLng;

    @Column(name = "distancia_na_captura_km")
    private Double captureDistanceKm;

    @Column(name = "percentual_no_caminho")
    private Double pathPercentage; // ex: 0.20 = 20%

    @Column(name = "capturada_em")
    private LocalDateTime capturedAt;
}
