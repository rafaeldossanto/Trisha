package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resposta de login repassada do Cadastro: o usuario autenticado e o access
 * token da app que o front deve enviar no header Authorization.
 */
public record AuthenticationResponse(
        @JsonProperty("usuario") UserResponse user,
        String accessToken,
        @JsonProperty("expiresInSegundos") long expiresInSeconds
) {}
