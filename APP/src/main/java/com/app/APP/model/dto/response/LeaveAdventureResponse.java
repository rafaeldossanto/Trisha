package com.app.APP.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultado de sair (ou ser removido) de uma aventura em grupo. Quando os dados
 * sao preservados, {@code aventuraPessoalId} aponta a aventura pessoal PRIVADA
 * criada com os caminhos/midias do usuario; quando descartados, ela e nula.
 */
public record LeaveAdventureResponse(
        @JsonProperty("aventuraPessoalId") String personalAdventureId,
        @JsonProperty("caminhosMovidos") int movedPaths,
        @JsonProperty("caminhosExcluidos") int deletedPaths
) {}
