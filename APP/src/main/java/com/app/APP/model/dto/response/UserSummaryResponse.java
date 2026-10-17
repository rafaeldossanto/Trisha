package com.app.APP.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

/**
 * Resumo de usuario com o id interno — para o BFF "traduzir" usuarioId em
 * nome/codigo ao montar mapa, ao vivo e feed. Nao expor direto ao front:
 * a visao publica continua sendo PublicUserResponse (sem id).
 */
@Builder
public record UserSummaryResponse(
        String id,
        @JsonProperty("nome") String name,
        @JsonProperty("codigoUsuario") String userCode
) {}
