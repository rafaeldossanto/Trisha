package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Login social repassado ao Cadastro. O provedor vai como String
 * (ex.: "GOOGLE", "APPLE") e e validado no Cadastro, que conhece o enum —
 * mantendo o BFF desacoplado dos enums dos servicos.
 */
public record SocialLoginRequest(
        @JsonProperty("provedor") @NotBlank String provider,
        @NotBlank String idToken
) {}
