package com.trail.Cadastro.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

import java.util.List;

import static java.util.Objects.nonNull;

/**
 * Cria um {@link org.springframework.security.oauth2.jwt.JwtDecoder} por
 * provedor social. O decoder e construido a partir do issuer (descoberta OIDC),
 * o que ja configura a busca do JWKS publico e a validacao de assinatura,
 * issuer e expiracao. Adicionamos a validacao de audience (client-id), que
 * garante que o token foi emitido para a NOSSA aplicacao.
 *
 * Issuers oficiais:
 *  - Google: https://accounts.google.com
 *  - Apple:  https://appleid.apple.com
 */
@Configuration
@EnableConfigurationProperties(SocialAuthProperties.class)
public class JwtDecoderConfig {

    private static final String ISSUER_GOOGLE = "https://accounts.google.com";
    private static final String ISSUER_APPLE = "https://appleid.apple.com";

    private final SocialAuthProperties properties;

    public JwtDecoderConfig(SocialAuthProperties properties) {
        this.properties = properties;
    }

    @Bean
    public NimbusJwtDecoder googleJwtDecoder() {
        return decoderWithAudience(ISSUER_GOOGLE, properties.getGoogle().getClientId());
    }

    @Bean
    public NimbusJwtDecoder appleJwtDecoder() {
        return decoderWithAudience(ISSUER_APPLE, properties.getApple().getClientId());
    }

    private NimbusJwtDecoder decoderWithAudience(String issuer, String expectedAudience) {
        NimbusJwtDecoder decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(issuer);

        OAuth2TokenValidator<Jwt> standard = JwtValidators.createDefaultWithIssuer(issuer);
        OAuth2TokenValidator<Jwt> audience = new JwtClaimValidator<List<String>>(
                JwtClaimNames.AUD,
                aud -> nonNull(aud) && aud.contains(expectedAudience));

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(standard, audience));
        return decoder;
    }
}
