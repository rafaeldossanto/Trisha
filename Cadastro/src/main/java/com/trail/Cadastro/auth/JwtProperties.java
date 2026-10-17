package com.trail.Cadastro.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametros do token de sessao emitido pela aplicacao (distinto do ID token
 * social dos provedores). O issuer identifica o Cadastro como emissor e e
 * validado pelos resource servers; o ttl define a validade do access token.
 *
 * <p>Em producao, defina {@code JWT_RSA_PRIVATE_KEY_PATH} apontando para um
 * arquivo PEM (PKCS#8) com a chave privada RSA — tipicamente injetado via
 * Secret Manager ou volume Kubernetes. Em dev, deixe em branco para usar a
 * chave de desenvolvimento incluida no classpath (nao e um segredo real).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {

    private String issuer = "http://localhost:8080";

    private long ttlSegundos = 7200;

    /**
     * Caminho para o arquivo PEM (PKCS#8) com a chave privada RSA.
     * Vazio em dev: usa classpath:keys/dev-private-key.pem.
     * Producao: defina via env var JWT_RSA_PRIVATE_KEY_PATH.
     */
    private String rsaPrivateKeyPath = "";
}
