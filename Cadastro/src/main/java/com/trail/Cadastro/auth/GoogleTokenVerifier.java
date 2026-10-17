package com.trail.Cadastro.auth;

import com.trail.Cadastro.model.enums.AuthProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Verifica ID tokens do Google. A validacao de assinatura (JWKS), issuer,
 * audience e expiracao e feita pelo NimbusJwtDecoder configurado em
 * JwtDecoderConfig; aqui so extraimos os claims relevantes.
 */
@Component
@Slf4j
public class GoogleTokenVerifier implements SocialTokenVerifier {

    private final NimbusJwtDecoder googleJwtDecoder;

    public GoogleTokenVerifier(@Qualifier("googleJwtDecoder") NimbusJwtDecoder googleJwtDecoder) {
        this.googleJwtDecoder = googleJwtDecoder;
    }

    @Override
    public AuthProvider provider() {
        return AuthProvider.GOOGLE;
    }

    @Override
    public ProviderUserData verify(String idToken) {
        try {
            Jwt jwt = googleJwtDecoder.decode(idToken);
            return new ProviderUserData(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("name"));
        } catch (JwtException e) {
            log.warn("ID token do Google invalido: {}", e.getMessage());
            throw new IllegalArgumentException("Token do Google invalido");
        }
    }
}
