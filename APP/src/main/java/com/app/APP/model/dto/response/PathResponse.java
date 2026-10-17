package com.app.APP.model.dto.response;

import com.app.APP.model.enums.Color;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PathResponse(
        String id,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("cor") Color color,
        @JsonProperty("numero") Integer number,
        @JsonProperty("iniciadoEm") LocalDateTime startedAt,
        @JsonProperty("finalizadoEm") LocalDateTime finishedAt,
        @JsonProperty("distanciaTotalKm") Double totalDistanceKm
) {}
