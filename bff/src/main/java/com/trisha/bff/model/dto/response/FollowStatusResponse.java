package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Relacao de seguir entre o token e outro usuario (mutuo libera adicionar amigo).
 */
public record FollowStatusResponse(
        @JsonProperty("sigo") boolean following,
        @JsonProperty("meSegue") boolean followsMe,
        @JsonProperty("mutuo") boolean mutual
) {}
