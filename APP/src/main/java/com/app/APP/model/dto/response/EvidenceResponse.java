package com.app.APP.model.dto.response;

import com.app.APP.model.enums.EvidenceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EvidenceResponse(
        String id,
        @JsonProperty("pontoId") String pointId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("fotoUrl") String photoUrl,
        @JsonProperty("tipoEvidencia") EvidenceType evidenceType,
        @JsonProperty("validada") Boolean validated,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
