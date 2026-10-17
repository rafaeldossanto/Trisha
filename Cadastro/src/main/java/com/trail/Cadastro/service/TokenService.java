package com.trail.Cadastro.service;

import com.trail.Cadastro.auth.IssuedToken;
import com.trail.Cadastro.auth.JwtProperties;
import com.trail.Cadastro.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;

/**
 * Emite o access token da aplicacao apos a autenticacao. O subject e o id
 * interno (UUID); o userCode vai como claim por ser o identificador
 * publico usado nas relacoes (amizades, sessoes).
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(properties.getTtlSegundos()))
                .subject(user.getId())
                .claim("codigoUsuario", user.getUserCode())
                .claim("email", user.getEmail())
                .claim("nome", user.getName())
                .build();

        String value = jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
        return new IssuedToken(value, properties.getTtlSegundos());
    }
}
