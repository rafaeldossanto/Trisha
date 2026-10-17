package com.app.APP.model.dto.response;

import com.app.APP.model.dto.request.CityDTO;
import com.app.APP.model.enums.RegionVisibility;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record RegionResponse(
        String id,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        @JsonProperty("capaUrl") String coverUrl,
        @JsonProperty("visibilidade") RegionVisibility visibility,
        @JsonProperty("cidades") List<CityDTO> cities,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
