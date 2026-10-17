package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Espelha a trilha por caminho do servico de Localizacao (consulta por bbox). */
public record TrailPointsResponse(
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("pontos") List<TrailPointResponse> points
) {}
