package com.trail.Cadastro.auth;

/**
 * Dados de identidade extraidos de um ID token validado de um provedor social.
 *
 * @param subject identificador unico e estavel do usuario no provedor (claim "sub")
 * @param email   email confirmado pelo provedor
 * @param name    nome de exibicao (pode vir nulo em alguns provedores/escopos)
 */
public record ProviderUserData(
        String subject,
        String email,
        String name
) {}
