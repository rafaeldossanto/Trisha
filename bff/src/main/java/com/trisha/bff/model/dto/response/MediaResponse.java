package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Visao de midia exposta pelo BFF ao front. Espelha os metadados de midia
 * (incluindo a URL do binario no storage) que o APP ja mantem — por isso o
 * BFF resolve "midias de uma aventura" batendo apenas no APP.
 */
public record MediaResponse(
        String id,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("tipo") String type,
        String url,
        @JsonProperty("percentualNoCaminho") Double pathPercentage,
        @JsonProperty("distanciaNaCapturaKm") Double captureDistanceKm,
        @JsonProperty("capturadaEm") LocalDateTime capturedAt
) {}
