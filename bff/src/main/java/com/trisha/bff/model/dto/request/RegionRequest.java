package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Criacao/edicao de regiao (pasta). visibilidade como String (o APP valida o
 * enum). O dono vem do token, nao do corpo.
 */
public record RegionRequest(
        @JsonProperty("nome") @NotBlank String name,
        @JsonProperty("descricao") String description,
        @JsonProperty("capaUrl") String coverUrl,
        @JsonProperty("visibilidade") String visibility,
        @JsonProperty("cidades") List<CityDTO> cities
) {}
