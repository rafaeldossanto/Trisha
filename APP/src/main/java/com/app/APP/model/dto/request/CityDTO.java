package com.app.APP.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Cidade de uma regiao (usado tanto no request quanto no response). Coordenadas
 * sao opcionais.
 */
public record CityDTO(
        @JsonProperty("nome") @NotBlank String name,
        Double latitude,
        Double longitude,
        Double altitude
) {}
