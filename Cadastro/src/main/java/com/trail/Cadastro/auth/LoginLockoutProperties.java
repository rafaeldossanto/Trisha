package com.trail.Cadastro.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros do bloqueio temporario de login por conta, sob o prefixo
 * "login-lockout". Apos {@code maxAttempts} falhas seguidas de senha para um
 * mesmo email, novas tentativas sao recusadas por {@code blockSeconds}. Defende
 * contra brute-force/credential-stuffing distribuido em muitos IPs — que o rate
 * limit por IP da borda nao pega sozinho.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "login-lockout")
public class LoginLockoutProperties {

    /** Liga/desliga o bloqueio (util em dev e testes). */
    private boolean enabled = true;

    /** Falhas seguidas que disparam o bloqueio. */
    private int maxAttempts = 5;

    /** Duracao do bloqueio — e da janela de contagem — em segundos. */
    private int blockSeconds = 900;
}
