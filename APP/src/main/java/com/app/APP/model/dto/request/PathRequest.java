package com.app.APP.model.dto.request;

import com.app.APP.model.enums.Color;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Inicio de caminho. Aventura e obrigatoria; cor e opcional (default ROXO).
 * O dono vem do token (nao do corpo). O numero do caminho NAO vem do cliente —
 * e gerado de forma sequencial por aventura no service.
 */
public record PathRequest(
        @JsonProperty("aventuraId") @NotBlank String adventureId,
        @JsonProperty("cor") Color color
) {}
