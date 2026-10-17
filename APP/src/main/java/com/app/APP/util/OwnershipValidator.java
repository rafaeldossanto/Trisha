package com.app.APP.util;

import com.app.APP.exception.ForbiddenException;
import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

/**
 * Utilitario para validar propriedade de recursos.
 *
 * Centraliza o padrao "verifica dono ou lanca excecao" que aparece em varios
 * servicos, evitando duplicacao da logica de checagem de userId.
 */
@UtilityClass
public class OwnershipValidator {

    /**
     * Valida que o ownerId do recurso coincide com o userId informado.
     * Lanca ForbiddenException (HTTP 403) com a mensagem fornecida caso contrario.
     *
     * @param userId    id do usuario autenticado
     * @param ownerId   id do dono registrado no recurso
     * @param message   mensagem de erro (ex.: "Voce nao e o dono desta aventura")
     */
    public static void requireOwner(String userId, String ownerId, String message) {
        if (!userId.equals(ownerId)) {
            throw new ForbiddenException(message);
        }
    }

    /**
     * Variante com supplier para mensagem lazy (evita concatenacao desnecessaria).
     */
    public static void requireOwner(String userId, String ownerId, Supplier<String> message) {
        if (!userId.equals(ownerId)) {
            throw new ForbiddenException(message.get());
        }
    }
}
