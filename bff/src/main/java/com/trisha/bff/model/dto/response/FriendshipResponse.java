package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Visao de amizade exposta pelo BFF ao front. Espelha os campos que o
 * front consome da resposta do servico APP. DTO proprio do BFF: desacopla
 * o contrato do front da estrutura interna do downstream.
 */
public record FriendshipResponse(
        String id,
        @JsonProperty("solicitanteId") String requesterId,
        @JsonProperty("receptorId") String receiverId,
        String status,
        @JsonProperty("solicitadoEm") LocalDateTime requestedAt,
        @JsonProperty("respondidoEm") LocalDateTime respondedAt
) {}
