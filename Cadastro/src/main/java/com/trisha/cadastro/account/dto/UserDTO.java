package com.trisha.cadastro.account.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.cadastro.account.RegistrationStatus;
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
