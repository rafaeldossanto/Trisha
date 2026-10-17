package com.trisha.app.social.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Contadores de seguidores/seguindo de um usuario. */
public record CountersResponse(
        @JsonProperty("seguidores") long followers,
        @JsonProperty("seguindo") long following
) {}
