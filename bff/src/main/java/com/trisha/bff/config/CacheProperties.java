package com.trisha.bff.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros de cache do BFF, sob o prefixo "cache" em application.yaml.
 */
@ConfigurationProperties(prefix = "cache")
@Getter
@Setter
public class CacheProperties {

    /** Tempo de vida das entradas de cache, em segundos. */
    private long ttlSegundos = 60;
}
