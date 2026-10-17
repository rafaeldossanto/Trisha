package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Espelha o resumo de usuario do APP (id -> nome/codigo). Uso interno do BFF
 * para enriquecer mapa, ao vivo e feed — nao e exposto como endpoint.
 */
public record UserSummaryResponse(
        String id,
        @JsonProperty("nome") String name,
        @JsonProperty("codigoUsuario") String userCode
) {}
