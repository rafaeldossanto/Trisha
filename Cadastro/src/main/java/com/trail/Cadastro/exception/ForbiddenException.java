package com.trail.Cadastro.exception;

/**
 * Acesso negado: o usuario autenticado tentou operar sobre uma conta que nao e a
 * sua. Mapeada para HTTP 403 pelo {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
