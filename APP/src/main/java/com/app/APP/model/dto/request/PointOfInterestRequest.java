package com.app.APP.model.dto.request;

import com.app.APP.model.enums.PointType;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Criacao de ponto de interesse. Nome e descricao sao opcionais (a descricao,
 * inclusive, eleva o nivel de confianca, mas nao e obrigatoria). Coordenadas
 * e tipo identificam o ponto e sao obrigatorios. O autor vem do token.
 */
public record PointOfInterestRequest(
        @JsonProperty("caminhoId") @NotBlank String pathId,
        @JsonProperty("tipo") @NotNull PointType type,
        @JsonProperty("nome") String name,
        @JsonProperty("descricao") String description,
        @NotNull Double latitude,
        @NotNull Double longitude
) {}
