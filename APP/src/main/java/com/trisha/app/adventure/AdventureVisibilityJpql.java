package com.trisha.app.adventure;

/**
 * A regra de visibilidade da aventura em JPQL, para as queries de listagem e
 * descoberta: PUBLICA para todos, SO_GRUPO so para participantes, PRIVADA para
 * ninguem. E a mesma regra do {@link AdventureAccessService#canView}, sem o caso
 * do dono: cada query decide o que fazer com as aventuras do proprio observador
 * ({@code OR a.userId = :observer} ou {@code a.userId <> :observer}).
 *
 * A query que usa o trecho precisa chamar a aventura de {@code a} e ter o
 * parametro {@code :observer}; o alias {@code ap} fica reservado ao participante.
 *
 * Classe comum com constante, e nao {@code @UtilityClass}: o valor de uma
 * annotation ({@code @Query}) precisa ser uma constante de compilacao.
 */
public final class AdventureVisibilityJpql {

    public static final String VISIBLE_TO_OBSERVER = """
            (a.visibility = com.trisha.app.adventure.AdventureVisibility.PUBLICA
             OR (a.visibility = com.trisha.app.adventure.AdventureVisibility.SO_GRUPO
                 AND EXISTS (SELECT 1 FROM AdventureParticipant ap
                             WHERE ap.adventure.id = a.id AND ap.userId = :observer)))""";

    private AdventureVisibilityJpql() {
    }
}
