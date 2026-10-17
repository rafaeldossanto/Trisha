package com.trail.Cadastro.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trail.Cadastro.model.enums.RegistrationStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UserDTO(
        String id,
        @JsonProperty("nome") String name,
        String email,
        @JsonProperty("codigoUsuario") String userCode,
        RegistrationStatus status,
        @JsonProperty("dataCriacao") LocalDateTime createdAt,
        @JsonProperty("dataAtualizacao") LocalDateTime updatedAt
) {
}
