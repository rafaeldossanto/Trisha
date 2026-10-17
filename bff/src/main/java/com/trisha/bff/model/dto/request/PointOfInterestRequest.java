package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PointOfInterestRequest(
        @JsonProperty("caminhoId") @NotBlank String pathId,
        @JsonProperty("tipo") @NotBlank String type,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
