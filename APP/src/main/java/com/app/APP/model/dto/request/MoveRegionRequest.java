package com.app.APP.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Move uma aventura entre pastas. regionId nulo = tirar a aventura da pasta.
 */
public record MoveRegionRequest(
        @JsonProperty("regiaoId") String regionId
) {}
