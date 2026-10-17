package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.bff.model.dto.request.CityDTO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Regiao = pasta do usuario. visibilidade como String (padrao do BFF — nao
 * duplica enums; o APP valida).
 */
public record RegionResponse(
        String id,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        @JsonProperty("capaUrl") String coverUrl,
        @JsonProperty("visibilidade") String visibility,
        @JsonProperty("cidades") List<CityDTO> cities,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
