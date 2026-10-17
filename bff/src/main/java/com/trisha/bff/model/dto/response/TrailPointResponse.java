package com.trisha.bff.model.dto.response;

/** Espelha o ponto enxuto de trilha do servico de Localizacao (consulta por bbox). */
public record TrailPointResponse(
        Double latitude,
        Double longitude,
        Double altitude
) {}
