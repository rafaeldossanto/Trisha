package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record GpsPointResponse(
        String id,
        @JsonProperty("sessaoId") String sessionId,
        Double latitude,
        Double longitude,
        Double altitude,
        @JsonProperty("precisao") Double accuracy,
        @JsonProperty("velocidade") Double speed,
        @JsonProperty("ordem") Integer order,
        @JsonProperty("registradoEm") LocalDateTime recordedAt,
        @JsonProperty("proximoDoInicio") Boolean nearStart,
        @JsonProperty("distanciaDoInicioMetros") Double distanceFromStartMeters
) {}
