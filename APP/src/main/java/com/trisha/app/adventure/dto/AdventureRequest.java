package com.trisha.app.adventure.dto;

import com.trisha.app.adventure.AdventureVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Criacao de aventura. region (pasta) e opcional — aventura pode existir solta;
 * destino e obrigatorio. Visibilidade opcional (entidade tem default PRIVADA).
 * O dono vem do token, nao do corpo.
 */
public record AdventureRequest(
        @JsonProperty("regiaoId") String regionId,
        @JsonProperty("destino") @NotBlank String destination,
        @JsonProperty("visibilidade") AdventureVisibility visibility
) {}
