package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDateTime;

/**
 * Espelha a sessao ao vivo do servico de Localizacao: quem esta trilhando
 * agora e o usuario pode acompanhar, com a ultima posicao para o marcador.
 * O acompanhamento em si e via WebSocket direto no loc (/ws-localizacao).
 */
public record LiveSessionResponse(
        @JsonProperty("sessaoId") String sessionId,
        @JsonProperty("caminhoId") String pathId,
        @JsonProperty("usuarioId") String userId,
        /** Preenchidos pelo BFF (o loc nao conhece usuarios). */
        @JsonProperty("usuarioNome") String userName,
        @JsonProperty("usuarioCodigo") String userCode,
        @JsonProperty("visibilidade") String visibility,
        @JsonProperty("iniciadaEm") LocalDateTime startedAt,
        Double latitude,
        Double longitude
) {}
