package com.trail.Cadastro.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Publica a chave publica (JWKS) usada para validar os tokens da aplicacao.
 * Endpoint publico consumido pelos resource servers via jwk-set-uri.
 */
@RestController
@RequiredArgsConstructor
public class JwksController {

    private final RSAKey rsaKey;

    @GetMapping("/oauth2/jwks")
    public Map<String, Object> jwks() {
        return new JWKSet(rsaKey.toPublicJWK()).toJSONObject();
    }
}
