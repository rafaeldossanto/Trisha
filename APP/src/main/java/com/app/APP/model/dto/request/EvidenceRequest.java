package com.app.APP.model.dto.request;

import com.app.APP.model.enums.EvidenceType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Adicao de evidencia a um ponto. As coordenadas de captura sao obrigatorias —
 * o service valida que estao a menos de 50m do ponto. evidenceType e opcional.
 * O autor vem do token.
 */
public record EvidenceRequest(
        @JsonProperty("pontoId") @NotBlank String pointId,
        @JsonProperty("fotoUrl") @NotBlank String photoUrl,
        @JsonProperty("tipoEvidencia") EvidenceType evidenceType,
        @JsonProperty("latCaptura") @NotNull Double captureLat,
        @JsonProperty("lngCaptura") @NotNull Double captureLng
) {}
