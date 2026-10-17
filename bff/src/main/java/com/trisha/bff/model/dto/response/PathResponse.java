package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record PathResponse(
        String id,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("cor") String color,
        @JsonProperty("numero") Integer number,
        @JsonProperty("iniciadoEm") LocalDateTime startedAt,
        @JsonProperty("finalizadoEm") LocalDateTime finishedAt,
        @JsonProperty("distanciaTotalKm") Double totalDistanceKm
) {}
