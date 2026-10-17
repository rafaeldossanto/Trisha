package com.trisha.bff.ratelimit;

/**
 * Resultado de uma checagem de rate limit: se a requisicao pode seguir, a
 * contagem atual na janela, o teto configurado e por quantos segundos o cliente
 * deve esperar antes de tentar de novo (para o header Retry-After).
 */
public record RateLimitResult(boolean allowed, long count, int limit, int retryAfterSeconds) {

    static RateLimitResult liberado(int limit) {
        return new RateLimitResult(true, 0, limit, 0);
    }
}
