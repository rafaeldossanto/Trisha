package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.LoginLockoutProperties;
import com.trail.Cadastro.exception.TooManyAttemptsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Bloqueio temporario de login por conta (email) apos varias falhas seguidas.
 * Contador por email no Redis com expiracao automatica (TTL) — some sozinho
 * quando a janela passa. Fail-open: se o Redis cair, nunca bloqueia um login
 * legitimo; a borda ainda limita por IP.
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final String PREFIXO = "login:fail:";

    // INCR no primeiro erro define o TTL da janela de bloqueio; retorna a contagem.
    private static final RedisScript<Long> REGISTRAR = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c", Long.class);

    private final StringRedisTemplate redis;
    private final LoginLockoutProperties properties;

    public LoginAttemptService(StringRedisTemplate redis, LoginLockoutProperties properties) {
        this.redis = redis;
        this.properties = properties;
    }

    /** Recusa o login com {@link TooManyAttemptsException} se o email ja estourou o teto. */
    public void assertNotBlocked(String email) {
        if (!properties.isEnabled()) {
            return;
        }
        Long falhas = falhasAtuais(chave(email));
        if (falhas != null && falhas >= properties.getMaxAttempts()) {
            log.warn("[LOCKOUT] Login bloqueado por excesso de tentativas: {}", email);
            throw new TooManyAttemptsException("Muitas tentativas de login. Tente novamente em alguns minutos.");
        }
    }

    /** Registra uma falha de senha para o email. */
    public void recordFailure(String email) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            redis.execute(REGISTRAR, List.of(chave(email)), String.valueOf(properties.getBlockSeconds()));
        } catch (RuntimeException ex) {
            log.warn("[LOCKOUT] Redis indisponivel ao registrar falha (ignorado): {}", ex.getMessage());
        }
    }

    /** Zera o contador apos um login bem-sucedido. */
    public void reset(String email) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            redis.delete(chave(email));
        } catch (RuntimeException ex) {
            log.warn("[LOCKOUT] Redis indisponivel ao resetar contador (ignorado): {}", ex.getMessage());
        }
    }

    private Long falhasAtuais(String chave) {
        try {
            String valor = redis.opsForValue().get(chave);
            return valor == null ? null : Long.parseLong(valor);
        } catch (RuntimeException ex) {
            log.warn("[LOCKOUT] Redis indisponivel ao consultar contador (fail-open): {}", ex.getMessage());
            return null;
        }
    }

    private String chave(String email) {
        return PREFIXO + email.trim().toLowerCase();
    }
}
