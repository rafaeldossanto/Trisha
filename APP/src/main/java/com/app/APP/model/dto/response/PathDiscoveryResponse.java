package com.app.APP.model.dto.response;

import com.app.APP.model.enums.Color;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Caminho visivel no mapa colaborativo (descoberta por bbox): so o que o app
 * precisa para desenhar e rotular a trilha de outro usuario. A geometria
 * (pontos GPS) vem do servico de Localizacao — o BFF junta as duas partes.
 */
@Builder
public record PathDiscoveryResponse(
        String id,
        @JsonProperty("aventuraId") String adventureId,
        @JsonProperty("usuarioId") String userId,
        @JsonProperty("destino") String destination,
        @JsonProperty("cor") Color color
) {}
