package com.trail.Cadastro.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;

/**
 * Atualizacao parcial: os campos sao opcionais. Um campo nulo significa
 * "manter o valor atual" — por isso nao ha @NotBlank aqui. Quando o email
 * vem preenchido, porem, precisa ser um email valido (@Email aceita null).
 */
public record UserUpdateRequest(
        @JsonProperty("nome") String name,
        @Email String email
) {
}
