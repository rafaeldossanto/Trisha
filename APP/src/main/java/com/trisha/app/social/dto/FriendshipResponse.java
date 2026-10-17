package com.trisha.app.social.dto;

import com.trisha.app.social.FriendshipStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FriendshipResponse(
        String id,
        @JsonProperty("solicitanteId") String requesterId,
        @JsonProperty("receptorId") String receiverId,
        FriendshipStatus status,
        @JsonProperty("bloqueadoPor") String blockedBy,
        @JsonProperty("solicitadoEm") LocalDateTime requestedAt,
        @JsonProperty("respondidoEm") LocalDateTime respondedAt
) {}
