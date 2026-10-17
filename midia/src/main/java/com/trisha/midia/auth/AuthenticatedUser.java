package com.trisha.midia.auth;

/**
 * Identidade do usuario autenticado, extraida dos claims do JWT validado.
 */
public record AuthenticatedUser(String id, String userCode, String email) {
}
