package com.trisha.bff.ratelimit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Limites de requisicoes por IP na borda, sob o prefixo "rate-limit". Janela
 * fixa: cada IP pode fazer ate {@code defaultLimit} (ou {@code authLimit} nos
 * endpoints de autenticacao) requisicoes a cada {@code windowSeconds} segundos.
 */
@ConfigurationProperties(prefix = "rate-limit")
@Getter
@Setter
public class RateLimitProperties {

    /** Liga/desliga a protecao (util em dev e testes). */
    private boolean enabled = true;

    /**
     * Requisicoes por IP em cada janela, nos endpoints gerais. Folgado de
     * proposito: sob CGNAT muitos usuarios de celular compartilham um mesmo IP
     * publico, entao o teto por IP e so anti-flood, nao um limite por usuario.
     */
    private int defaultLimit = 600;

    /**
     * Requisicoes por IP em cada janela, nos endpoints de autenticacao. A defesa
     * precisa contra brute-force e o lockout por conta no Cadastro (chaveado por
     * email); aqui o teto por IP so contem flood.
     */
    private int authLimit = 30;

    /** Tamanho da janela, em segundos. */
    private int windowSeconds = 60;
}
