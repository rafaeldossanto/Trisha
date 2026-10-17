package com.trail.Cadastro.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;


public record UserCreateRequest(
        @JsonProperty("nome") @NotBlank String name,
        @NotBlank @Email String email,
        @JsonProperty("senha") @NotBlank String password
) {
}
