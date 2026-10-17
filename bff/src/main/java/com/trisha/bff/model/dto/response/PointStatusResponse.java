package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Marcacao pessoal do usuario num ponto de interesse: status de progressao
 * (NO_RADAR/NA_MIRA/CONQUISTADO, nulo se nao marcado) e a flag de objetivo.
 */
public record PointStatusResponse(
        @JsonProperty("pontoId") String pointId,
        String status,
        @JsonProperty("objetivo") boolean goal
) {}
