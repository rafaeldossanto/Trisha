package com.trisha.midia.exception;

/**
 * Acesso negado: o usuario autenticado nao e dono do arquivo que tentou operar.
 * Mapeada para HTTP 403 pelo {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
