package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Resposta agregada da "tela de aventura": junta a aventura, seus caminhos e
 * suas midias numa unica resposta. Evita que o app faca varias chamadas
 * sequenciais para montar a tela — uma unica chamada ao BFF traz tudo pronto.
 */
public record AdventureDetailResponse(
        @JsonProperty("aventura") AdventureResponse adventure,
        @JsonProperty("caminhos") List<PathResponse> paths,
        @JsonProperty("midias") List<MediaResponse> media
) {}
