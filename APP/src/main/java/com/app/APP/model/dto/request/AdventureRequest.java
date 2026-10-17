package com.app.APP.model.dto.request;

import com.app.APP.model.enums.AdventureVisibility;
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
