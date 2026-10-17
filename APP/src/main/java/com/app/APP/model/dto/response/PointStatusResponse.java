package com.app.APP.model.dto.response;

import com.app.APP.model.enums.PointStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record PointStatusResponse(
        @JsonProperty("pontoId") String pointId,
        PointStatus status,
        @JsonProperty("objetivo") boolean goal
) {}
