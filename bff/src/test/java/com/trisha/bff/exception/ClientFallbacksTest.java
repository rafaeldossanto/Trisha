package com.trisha.bff.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ClientFallbacks")
class ClientFallbacksTest {

    private static final String MESSAGE = "Servico temporariamente indisponivel.";

    @Test
    @DisplayName("erro de negocio do downstream (4xx) propaga com o status real")
    void devePropagar4xx() {
        var badRequest = new HttpClientErrorException(HttpStatus.BAD_REQUEST);

        RuntimeException result = ClientFallbacks.unavailable(badRequest, MESSAGE);

        assertThat(result).isSameAs(badRequest);
    }

    @Test
    @DisplayName("falha de infraestrutura (I/O) vira ServiceUnavailableException")
    void deveTraduzirFalhaDeIo() {
        var io = new ResourceAccessException("Connection refused");

        RuntimeException result = ClientFallbacks.unavailable(io, MESSAGE);

        assertThat(result)
                .isInstanceOf(ServiceUnavailableException.class)
                .hasMessage(MESSAGE);
    }

    @Test
    @DisplayName("erro do downstream (5xx) tambem vira ServiceUnavailableException")
    void deveTraduzir5xx() {
        var serverError = new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);

        RuntimeException result = ClientFallbacks.unavailable(serverError, MESSAGE);

        assertThat(result).isInstanceOf(ServiceUnavailableException.class);
    }
}
