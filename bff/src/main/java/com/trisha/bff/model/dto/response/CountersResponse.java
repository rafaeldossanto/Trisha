package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CountersResponse(
        @JsonProperty("seguidores") long followers,
        @JsonProperty("seguindo") long following
) {}
