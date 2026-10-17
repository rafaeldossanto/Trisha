package com.trail.Cadastro.model.event;

/**
 * Publicado quando um cadastro e criado. O envio do email de confirmacao
 * escuta este evento apos o commit — ver UserRegisteredListener.
 */
public record UserRegisteredEvent(String userId, String email) {
}
