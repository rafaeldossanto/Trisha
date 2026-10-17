package com.trisha.app.point.dto;

import com.trisha.app.point.PointStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record PointStatusResponse(
        @JsonProperty("pontoId") String pointId,
        PointStatus status,
        @JsonProperty("objetivo") boolean goal
) {}
