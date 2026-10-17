package com.app.APP.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Visao publica de um usuario, exposta na busca para adicionar amigos.
 * Inclui apenas o que outro usuario pode ver — nome e codigoUsuario.
 * NAO expoe email nem id interno, por privacidade.
 */
@Builder
public record PublicUserResponse(
        @JsonProperty("codigoUsuario") String userCode,
        @JsonProperty("nome") String name
) {}
