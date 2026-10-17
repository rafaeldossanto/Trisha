package com.trisha.bff.model.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Pagina generica do BFF. Os servicos downstream (APP) serializam um
 * {@link org.springframework.data.domain.Page} do Spring Data com os campos
 * {@code content}, {@code number}, {@code size}, {@code totalElements} e
 * {@code totalPages}. Desserializar diretamente em {@code Page} via RestClient
 * e fragil (PageImpl nao tem construtor padrao), entao mapeamos esse formato
 * para um record simples que o front consome.
 */
public record PageResponse<T>(
        @JsonProperty("content") List<T> content,
        @JsonProperty("number") int number,
        @JsonProperty("size") int size,
        @JsonProperty("totalElements") long totalElements,
        @JsonProperty("totalPages") int totalPages
) {
    /** Fallback vazio usado pelos circuit breakers ao retornar listas degradadas. */
    public static <T> PageResponse<T> empty() {
        return new PageResponse<>(List.of(), 0, 0, 0, 0);
    }
}
