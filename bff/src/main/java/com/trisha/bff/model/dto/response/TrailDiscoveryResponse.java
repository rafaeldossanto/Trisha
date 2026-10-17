package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Trilha do mapa colaborativo entregue ao front: o metadado do caminho (do APP,
 * ja filtrado por visibilidade) casado com a geometria decimada (da
 * Localizacao). Uma entrada = uma polyline no mapa.
 */
public record TrailDiscoveryResponse(
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("usuarioNome") String userName,
        @JsonProperty("usuarioCodigo") String userCode,
        @JsonProperty("destino") String destination,
        @JsonProperty("cor") String color,
        @JsonProperty("pontos") List<TrailPointResponse> points
) {}
