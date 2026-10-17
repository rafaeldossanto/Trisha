package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Item do feed: a aventura ja enriquecida com nome/codigo do autor, para o
 * app montar o post sem chamadas extras.
 */
public record FeedAdventureResponse(
        String id,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("usuarioNome") String userName,
        @JsonProperty("usuarioCodigo") String userCode,
        @JsonProperty("regiaoId") String regionId,
        @JsonProperty("destino") String destination,
        String status,
        @JsonProperty("visibilidade") String visibility,
        @JsonProperty("criadoEm") LocalDateTime createdAt,
        @JsonProperty("participantes") int participantsCount,
        @JsonProperty("duracaoHoras") Double durationHours
) {}
