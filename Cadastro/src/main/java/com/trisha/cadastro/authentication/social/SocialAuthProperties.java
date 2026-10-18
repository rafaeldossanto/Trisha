package com.trisha.cadastro.authentication.social;

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

    private Provider google = new Provider();
    private Provider apple = new Provider();

    @Getter
    @Setter
    public static class Provider {
        /** Client ID (audience) esperado no ID token deste provedor. */
        private String clientId;
    }
}
