package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Visao publica de usuario (busca para adicionar amigos). Espelha o DTO do APP:
 * so nome e codigoUsuario, sem email nem id interno.
 */
public record PublicUserResponse(
        @JsonProperty("codigoUsuario") String userCode,
        @JsonProperty("nome") String name
) {}
