package com.trisha.bff.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Trava de borda contra flood e brute-force: limita requisicoes por IP numa
 * janela fixa (Redis) e responde 429 quando o teto e excedido. Registrado via
 * {@link RateLimitConfig} para rodar depois do TraceIdFilter (o 429 sai
 * correlacionado no log) e antes do Spring Security (nao gasta validacao de
 * token com trafego abusivo). Endpoints de autenticacao tem teto mais baixo —
 * sao o alvo classico de brute-force e cada tentativa custa um BCrypt no Cadastro.
 */
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiter rateLimiter;
    private final RateLimitProperties properties;

    public RateLimitFilter(RateLimiter rateLimiter, RateLimitProperties properties) {
        this.rateLimiter = rateLimiter;
        this.properties = properties;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!properties.isEnabled() || isExempt(request)) {
            chain.doFilter(request, response);
            return;
        }

        boolean authSensitive = isAuthSensitive(request);
        String scope = authSensitive ? "auth" : "geral";
        int limit = authSensitive ? properties.getAuthLimit() : properties.getDefaultLimit();
        String ip = clientIp(request);

        RateLimitResult result = rateLimiter.hit("rl:" + scope + ":" + ip, limit, properties.getWindowSeconds());
        if (!result.allowed()) {
            log.warn("[RATE-LIMIT] IP {} excedeu o limite ({}) no escopo {}", ip, limit, scope);
            reject(response, result.retryAfterSeconds());
            return;
        }
        chain.doFilter(request, response);
    }

    /** Health e documentacao nao entram na conta — monitoracao nao deve ser barrada. */
    private boolean isExempt(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs");
    }

    /** Endpoints de autenticacao/cadastro: alvo de brute-force e custosos (email/BCrypt). */
    private boolean isAuthSensitive(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path.startsWith("/bff/auth") || path.equals("/bff/usuarios/login-social") || path.endsWith("/reenviar-email")) {
            return true;
        }
        return "POST".equalsIgnoreCase(request.getMethod()) && path.equals("/bff/usuarios");
    }

    /**
     * Usa o IP remoto da conexao. Atras de um reverse proxy, habilite
     * {@code server.forward-headers-strategy=framework} para que este valor
     * reflita o IP real do cliente (X-Forwarded-For) em vez do IP do proxy.
     */
    private String clientIp(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    private void reject(HttpServletResponse response, int retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        // JSON montado a mao: sem entrada do usuario no corpo, nada a escapar.
        String body = String.format(
                "{\"status\":%d,\"mensagem\":\"Muitas requisicoes. Tente novamente em instantes.\",\"timestamp\":\"%s\"}",
                HttpStatus.TOO_MANY_REQUESTS.value(), LocalDateTime.now());
        response.getWriter().write(body);
    }
}
