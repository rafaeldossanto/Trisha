package com.app.APP.exception;

/**
 * Acesso negado: o usuario autenticado nao e dono nem parte do recurso que
 * tentou operar. Mapeada para HTTP 403 pelo {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
