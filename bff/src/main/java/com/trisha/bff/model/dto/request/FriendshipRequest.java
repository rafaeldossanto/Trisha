package com.trisha.bff.model.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * O solicitante NAO vem no request — e derivado do token (propagado downstream).
 * O cliente informa apenas o alvo.
 */
public record FriendshipRequest(
        @JsonProperty("receptorCodigo") @NotBlank String receiverCode
) {}
