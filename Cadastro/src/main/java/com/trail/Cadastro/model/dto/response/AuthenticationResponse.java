package com.trail.Cadastro.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Resposta do login: o usuario autenticado e o access token da aplicacao que o
 * app deve enviar no header Authorization das proximas chamadas.
 */
@Builder
public record AuthenticationResponse(
        @JsonProperty("usuario") UserDTO user,
        String accessToken,
        @JsonProperty("expiresInSegundos") long expiresInSeconds
) {
}
