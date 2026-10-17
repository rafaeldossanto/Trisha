package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record PathRequest(
        @JsonProperty("aventuraId") @NotBlank String adventureId,
        @JsonProperty("cor") String color
) {}
