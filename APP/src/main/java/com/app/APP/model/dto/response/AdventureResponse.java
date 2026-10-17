package com.app.APP.model.dto.response;

import com.app.APP.model.enums.AdventureStatus;
import com.app.APP.model.enums.AdventureVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record AdventureResponse(
        String id,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("regiaoId") String regionId,
        @JsonProperty("destino") String destination,
        AdventureStatus status,
        @JsonProperty("visibilidade") AdventureVisibility visibility,
        @JsonProperty("criadoEm") LocalDateTime createdAt,
        @JsonProperty("participantes") int participantsCount,
        @JsonProperty("duracaoHoras") Double durationHours
) {}
