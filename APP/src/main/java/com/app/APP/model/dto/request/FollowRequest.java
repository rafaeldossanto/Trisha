package com.app.APP.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Alvo de seguir/deixar de seguir, pelo codigoUsuario (no corpo — evita o '#'
 * no path). O seguidor vem do token.
 */
public record FollowRequest(
        @JsonProperty("seguidoCodigo") @NotBlank String followedCode
) {}
