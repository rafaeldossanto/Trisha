package com.trail.Cadastro.exception;

/** Bloqueio temporario por excesso de tentativas de login (mapeado para HTTP 429). */
public class TooManyAttemptsException extends RuntimeException {

    public TooManyAttemptsException(String message) {
        super(message);
    }
}
