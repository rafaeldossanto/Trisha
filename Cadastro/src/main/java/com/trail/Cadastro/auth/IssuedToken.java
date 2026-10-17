package com.trail.Cadastro.auth;

/**
 * Resultado da emissao de um access token: o valor assinado e sua validade em
 * segundos. Valor de dominio do auth — o DTO de resposta da API e montado a
 * partir dele no mapper.
 */
public record IssuedToken(String value, long expiresInSeconds) {
}
