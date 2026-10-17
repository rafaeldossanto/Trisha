package com.app.APP.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * O ator (solicitante/bloqueador) NAO vem no request — e derivado do token.
 * O cliente informa o alvo pelo codigoUsuario (handle publico); o service
 * resolve para o id interno. Assim nao expomos o UUID na busca de usuarios.
 */
public record FriendshipRequest(
        @JsonProperty("receptorCodigo") @NotBlank String receiverCode
) {}
