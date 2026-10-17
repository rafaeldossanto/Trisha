package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record SessionResponse(
        String id,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("usuarioId") String userId,
        String status,
        @JsonProperty("visibilidade") String visibility,
        @JsonProperty("terminoAutomatico") Boolean autoFinish,
        @JsonProperty("distanciaTerminoMetros") Double finishDistanceMeters,
        @JsonProperty("distanciaTotalKm") Double totalDistanceKm,
        @JsonProperty("iniciadaEm") LocalDateTime startedAt,
        @JsonProperty("finalizadaEm") LocalDateTime finishedAt
) {}
