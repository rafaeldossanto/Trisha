package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Move uma aventura entre pastas. regionId nulo = tirar da pasta.
 */
public record MoveRegionRequest(
        @JsonProperty("regiaoId") String regionId
) {}
