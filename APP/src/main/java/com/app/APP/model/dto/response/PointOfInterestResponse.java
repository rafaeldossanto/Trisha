package com.app.APP.model.dto.response;

import com.app.APP.model.enums.PointType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PointOfInterestResponse(
        String id,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("tipo") PointType type,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        Double latitude,
        Double longitude,
        @JsonProperty("nivelConfianca") Integer confidenceLevel,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
