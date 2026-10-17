package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record GpsPointRequest(
        @JsonProperty("sessaoId") @NotBlank String sessionId,
        @NotNull Double latitude,
        @NotNull Double longitude,
        Double altitude,
        @JsonProperty("precisao") Double accuracy,
        @JsonProperty("velocidade") Double speed
) {}
