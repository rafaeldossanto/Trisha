package com.app.APP.auth;

/**
 * Identity of the authenticated user, extracted from validated JWT claims.
 * Injected into controllers by {@link AuthenticatedUserArgumentResolver} —
 * the actor never comes from the request body or path.
 */
public record AuthenticatedUser(String id, String userCode, String email) {
}
