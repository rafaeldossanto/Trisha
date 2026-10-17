package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Espelha o caminho descobrivel do APP (enum de cor como String, padrao do BFF). */
public record PathDiscoveryResponse(
        String id,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("destino") String destination,
        @JsonProperty("cor") String color
) {}
