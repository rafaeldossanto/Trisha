package com.trisha.bff.exception;

/**
 * Acesso negado na borda: o usuario autenticado tentou operar sobre um recurso
 * que nao e seu. Mapeada para HTTP 403 pelo {@link GlobalExceptionHandler}.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
