package com.trisha.cadastro.authentication.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.cadastro.authentication.social.AuthProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Requisicao de login social: o app ja obteve o ID token do provedor
 * (Google/Apple) e o envia aqui para o backend validar e criar/vincular a conta.
 */
public record SocialLoginRequest(
        @JsonProperty("provedor") @NotNull AuthProvider provider,
        @NotBlank String idToken
) {}
