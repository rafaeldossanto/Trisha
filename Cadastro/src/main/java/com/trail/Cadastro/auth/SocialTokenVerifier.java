package com.trail.Cadastro.auth;

import com.trail.Cadastro.model.enums.AuthProvider;

/**
 * Verifica e extrai a identidade de um ID token emitido por um provedor social.
 *
 * Cada provedor (Google, Apple, ...) tem uma implementacao que valida a
 * assinatura do token contra o JWKS publico do provedor, alem de issuer,
 * audience e expiracao. So depois de validado o token e considerado confiavel.
 *
 * Para adicionar um provedor novo, basta criar uma implementacao que responda
 * pelo seu {@link AuthProvider} — o SocialAuthenticationService seleciona o
 * verificador certo pelo provedor informado.
 */
public interface SocialTokenVerifier {

    /** Provedor que esta implementacao atende. */
    AuthProvider provider();

    /**
     * Valida o ID token e devolve a identidade do usuario.
     *
     * @throws IllegalArgumentException se o token for invalido (assinatura,
     *         issuer, audience ou expiracao).
     */
    ProviderUserData verify(String idToken);
}
