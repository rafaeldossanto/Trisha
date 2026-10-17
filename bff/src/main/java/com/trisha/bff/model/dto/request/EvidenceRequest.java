package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EvidenceRequest(
        @JsonProperty("pontoId") @NotBlank String pointId,
        @JsonProperty("fotoUrl") @NotBlank String photoUrl,
        @JsonProperty("tipoEvidencia") @NotBlank String evidenceType,
        @JsonProperty("latCaptura") @NotNull Double captureLat,
        @JsonProperty("lngCaptura") @NotNull Double captureLng
) {}
