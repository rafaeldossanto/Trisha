package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Visao de usuario exposta pelo BFF. O status (enum no servico Cadastro) e
 * representado como String — o BFF nao duplica os enums dos servicos.
 */
public record UserResponse(
        String id,
        @JsonProperty("nome") String name,
        String email,
        @JsonProperty("codigoUsuario") String userCode,
        String status,
        @JsonProperty("dataCriacao") LocalDateTime createdAt,
        @JsonProperty("dataAtualizacao") LocalDateTime updatedAt
) {}
