package com.app.APP.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Relacao de seguir entre o usuario do token e outro: se eu o sigo, se ele me
 * segue e se e mutuo (mutuo libera o botao de adicionar amigo).
 */
public record FollowStatusResponse(
        @JsonProperty("sigo") boolean following,
        @JsonProperty("meSegue") boolean followsMe,
        @JsonProperty("mutuo") boolean mutual
) {}
