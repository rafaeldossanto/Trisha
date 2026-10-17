package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record AdventureResponse(
        String id,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("regiaoId") String regionId,
        @JsonProperty("destino") String destination,
        String status,
        @JsonProperty("visibilidade") String visibility,
        @JsonProperty("criadoEm") LocalDateTime createdAt,
        @JsonProperty("participantes") int participantsCount,
        @JsonProperty("duracaoHoras") Double durationHours
) {}
