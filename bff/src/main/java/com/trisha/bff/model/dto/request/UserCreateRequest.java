package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request de criacao de usuario, repassado ao servico Cadastro. As validacoes
 * (@NotBlank/@Email) barram payloads invalidos ja no BFF, antes do hop de rede.
 */
public record UserCreateRequest(
        @JsonProperty("nome") @NotBlank String name,
        @NotBlank @Email String email,
        @JsonProperty("senha") @NotBlank String password
) {}
