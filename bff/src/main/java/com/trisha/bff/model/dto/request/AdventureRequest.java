package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * A visibilidade e enviada como String (ex.: "PRIVADA", "PUBLICA") e validada
 * no servico APP, que conhece o enum VisibilidadeAventura. Manter String aqui
 * desacopla o BFF de duplicar os enums dos servicos.
 */
public record AdventureRequest(
        @JsonProperty("regiaoId") String regionId,
        @JsonProperty("destino") @NotBlank String destination,
        @JsonProperty("visibilidade") String visibility
) {}
