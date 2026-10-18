package com.trisha.app.shared.util;

import lombok.experimental.UtilityClass;

import java.util.function.Function;

import static java.util.Objects.isNull;

/**
 * Utilitarios compartilhados entre os mappers do APP.
 *
 * Centraliza helpers de mapeamento para evitar duplicacao inline.
 */
@UtilityClass
public class MapperUtils {

    /**
     * Extrai o id de um objeto que pode ser null.
     * Retorna null quando o objeto for null, ou chama getId() caso contrario.
     *
     * Exemplo de uso:
     * <pre>
     *   .pathId(idOrNull(media.getPath()))
     *   .regionId(idOrNull(adventure.getRegion()))
     * </pre>
     *
     * @param entity objeto com metodo getId(), pode ser null
     * @param <T>    tipo da entidade
     * @return id da entidade ou null
     */
    public static <T> String idOrNull(T entity, Function<T, String> idGetter) {
        return isNull(entity) ? null : idGetter.apply(entity);
    }
}
