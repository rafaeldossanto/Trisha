package com.trisha.midia.exception;

/**
 * Limite de upload do usuario estourado — taxa por hora ou armazenamento total.
 * Vira HTTP 429 no {@link GlobalExceptionHandler}.
 */
public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(String message) {
        super(message);
    }
}
