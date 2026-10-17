package com.trisha.midia.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.trisha.midia.model.enums.FileType;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record FileResponse(
        String id,
        @JsonProperty("nomeOriginal") String originalName,
        @JsonProperty("tipo") FileType type,
        String url,
        @JsonProperty("tamanhoBytes") Long sizeBytes,
        @JsonProperty("criadoEm") LocalDateTime createdAt
) {}
