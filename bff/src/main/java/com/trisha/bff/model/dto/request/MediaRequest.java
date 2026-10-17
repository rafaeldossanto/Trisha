package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Metadados de midia repassados ao APP. O 'caminhoId' e opcional (midia avulsa
 * na aventura); o 'url' ja aponta para o binario no storage (MinIO/R2),
 * subido diretamente pelo front no servico de Midia.
 */
public record MediaRequest(
        @JsonProperty("aventuraId") @NotBlank String adventureId,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("tipo") @NotBlank String type,
        @NotBlank String url,
        @JsonProperty("latCaptura") Double captureLat,
        @JsonProperty("lngCaptura") Double captureLng,
        @JsonProperty("distanciaNaCapturaKm") Double captureDistanceKm,
        @JsonProperty("percentualNoCaminho") Double pathPercentage
) {}
