package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PointOfInterestResponse(
        String id,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("tipo") String type,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        Double latitude,
        Double longitude,
        @JsonProperty("nivelConfianca") Integer confidenceLevel,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
