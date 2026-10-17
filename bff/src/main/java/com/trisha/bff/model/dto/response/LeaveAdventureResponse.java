package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Resultado de sair (ou ser removido) de uma aventura em grupo, espelhado do
 * servico APP: aventura pessoal criada com os dados preservados (nula quando
 * descartados) e a contagem de caminhos movidos/excluidos.
 */
public record LeaveAdventureResponse(
        @JsonProperty("aventuraPessoalId") String personalAdventureId,
        @JsonProperty("caminhosMovidos") int movedPaths,
        @JsonProperty("caminhosExcluidos") int deletedPaths
) {}
