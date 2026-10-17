package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

public record EvidenceResponse(
        String id,
        @JsonProperty("pontoId") String pointId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("fotoUrl") String photoUrl,
        @JsonProperty("tipoEvidencia") String evidenceType,
        @JsonProperty("validada") Boolean validated,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
