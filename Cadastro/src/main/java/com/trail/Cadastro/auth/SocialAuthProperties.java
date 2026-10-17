package com.trail.Cadastro.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracao dos provedores de login social, sob o prefixo "auth.social"
 * em application.yaml. O client-id (audience esperado no token) e o ponto
 * critico: garante que o token foi emitido PARA a nossa aplicacao, e nao
 * para outra que por acaso usa o mesmo provedor.
 */
@ConfigurationProperties(prefix = "auth.social")
@Getter
@Setter
public class SocialAuthProperties {

    private Provedor google = new Provedor();
    private Provedor apple = new Provedor();

    @Getter
    @Setter
    public static class Provedor {
        /** Client ID (audience) esperado no ID token deste provedor. */
        private String clientId;
    }
}
