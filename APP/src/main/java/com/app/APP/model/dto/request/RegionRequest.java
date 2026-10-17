package com.app.APP.model.dto.request;

import com.app.APP.model.enums.RegionVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Criacao/edicao de regiao (pasta). O dono vem do token. Visibilidade nula vira
 * PRIVADA (default seguro). cities pode vir vazia.
 */
public record RegionRequest(
        @JsonProperty("nome") @NotBlank String name,
        @JsonProperty("descricao") String description,
        @JsonProperty("capaUrl") String coverUrl,
        @JsonProperty("visibilidade") RegionVisibility visibility,
        @JsonProperty("cidades") @Valid List<CityDTO> cities
) {}
