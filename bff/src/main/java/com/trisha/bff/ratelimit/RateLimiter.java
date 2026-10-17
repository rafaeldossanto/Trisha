package com.trisha.bff.ratelimit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Contador de requisicoes por janela fixa no Redis (INCR + EXPIRE atomico via
 * Lua), com estado compartilhado entre as instancias do BFF. Em caso de falha
 * do Redis adota fail-open: nunca derruba uma requisicao legitima so porque a
 * propria protecao ficou indisponivel.
 */
@Component
@Slf4j
public class RateLimiter {

    // INCR no primeiro hit define o TTL da janela; retorna a contagem atual.
    private static final RedisScript<Long> INCREMENTAR = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c", Long.class);

    private final StringRedisTemplate redis;

    public RateLimiter(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public RateLimitResult hit(String key, int limit, int windowSeconds) {
        try {
            Long count = redis.execute(INCREMENTAR, List.of(key), String.valueOf(windowSeconds));
            long atual = count == null ? 0 : count;
            return new RateLimitResult(atual <= limit, atual, limit, windowSeconds);
        } catch (RuntimeException ex) {
            log.warn("[RATE-LIMIT] Redis indisponivel, liberando requisicao (fail-open): {}", ex.getMessage());
            return RateLimitResult.liberado(limit);
        }
    }
}
