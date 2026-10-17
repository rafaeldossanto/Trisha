package com.app.APP.model.dto.response;

import com.app.APP.model.enums.MediaType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record MediaResponse(
        String id,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("tipo") MediaType type,
        String url,
        @JsonProperty("percentualNoCaminho") Double pathPercentage,
        @JsonProperty("distanciaNaCapturaKm") Double captureDistanceKm,
        @JsonProperty("capturadaEm") LocalDateTime capturedAt
) {}
