package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Cidade de uma regiao (request e response). Coordenadas opcionais.
 */
public record CityDTO(
        @JsonProperty("nome") String name,
        Double latitude,
        Double longitude,
        Double altitude
) {}
