package com.trail.Cadastro.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Atalho de login para desenvolvimento (profile dev): cria/recupera o usuario
 * pelo email e emite o token da app, sem provedor social. Nao existe em prod —
 * o controller e {@code @Profile("dev")}.
 */
public record DevLoginRequest(
        @NotBlank @Email String email,
        @JsonProperty("nome") @NotBlank String name
) {}
