package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * autoFinish, finishDistanceMeters e visibility sao opcionais. Quando nulos,
 * o servico de Localizacao aplica os defaults (desligado, 5m, PRIVADO). A
 * visibilidade define quem acompanha a trilha ao vivo:
 * PUBLICO/SEGUIDORES/AMIGOS/PRIVADO (enum como String, padrao do BFF).
 */
public record SessionRequest(
        @JsonProperty("caminhoId") @NotBlank String pathId,
        @JsonProperty("usuarioId") @NotBlank String userId,
        @JsonProperty("terminoAutomatico") Boolean autoFinish,
        @JsonProperty("distanciaTerminoMetros") Double finishDistanceMeters,
        @JsonProperty("visibilidade") String visibility
) {}
