package com.app.APP.model.dto.request;

import com.app.APP.model.enums.MediaType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Salva os metadados de uma midia. pathId e opcional (midia avulsa na
 * aventura); os campos de captura GPS sao opcionais. aventura, tipo e url
 * (do binario ja no storage) sao obrigatorios. O autor vem do token.
 */
public record MediaRequest(
        @JsonProperty("aventuraId") @NotBlank String adventureId,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("tipo") @NotNull MediaType type,
        @NotBlank String url,
        @JsonProperty("latCaptura") Double captureLat,
        @JsonProperty("lngCaptura") Double captureLng,
        @JsonProperty("distanciaNaCapturaKm") Double captureDistanceKm,
        @JsonProperty("percentualNoCaminho") Double pathPercentage
) {}
