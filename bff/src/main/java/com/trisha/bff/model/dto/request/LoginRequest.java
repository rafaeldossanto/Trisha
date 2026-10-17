package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/** Credenciais do login por email e senha, repassadas ao Cadastro. */
public record LoginRequest(
        @NotBlank @Email String email,
        @JsonProperty("senha") @NotBlank String password
) {}
