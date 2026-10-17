package com.app.APP.repository;

import com.app.APP.entity.Path;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PathRepository extends JpaRepository<Path, String> {
    Page<Path> findByAdventureId(String adventureId, Pageable pageable);
    Page<Path> findByUserId(String userId, Pageable pageable);

    /** Caminhos de um usuario dentro de uma aventura — usado ao sair/remover da aventura. */
    List<Path> findByAdventureIdAndUserId(String adventureId, String userId);

    /** How many paths the adventure already has — base for the next sequential number. */
    int countByAdventureId(String adventureId);

    /** Janela de tempo por aventura (min inicio / max fim) — um id ou em lote, evita N+1. */
    @Query("SELECT p.adventure.id, MIN(p.startedAt), MAX(p.finishedAt) FROM Path p WHERE p.adventure.id IN :ids GROUP BY p.adventure.id")
    List<Object[]> findTimespansByAdventureIds(List<String> ids);

    /** Someone else's paths the observer may see (owner uses findByUserId). */
    @Query(value = """
            SELECT p FROM Path p JOIN p.adventure a
            WHERE p.userId = :owner AND (
                a.visibility = com.app.APP.model.enums.AdventureVisibility.PUBLICA
                OR (a.visibility = com.app.APP.model.enums.AdventureVisibility.SO_GRUPO
                    AND EXISTS (SELECT 1 FROM AdventureParticipant part
                                WHERE part.adventure.id = a.id AND part.userId = :observer))
            )
            """,
            countQuery = """
            SELECT COUNT(p) FROM Path p JOIN p.adventure a
            WHERE p.userId = :owner AND (
                a.visibility = com.app.APP.model.enums.AdventureVisibility.PUBLICA
                OR (a.visibility = com.app.APP.model.enums.AdventureVisibility.SO_GRUPO
                    AND EXISTS (SELECT 1 FROM AdventureParticipant part
                                WHERE part.adventure.id = a.id AND part.userId = :observer))
            )
            """)
    Page<Path> findVisibleByUser(String owner, String observer, Pageable pageable);

    /**
     * Among the given ids, the paths whose adventure the observer may see on the
     * collaborative map: PUBLICA for everyone, SO_GRUPO only if they participate.
     * The observer's own paths are excluded — the app already plots them.
     * Same visibility semantics as AdventureRepository.findVisibleInRegion.
     */
    @Query("""
            SELECT p FROM Path p JOIN FETCH p.adventure a
            WHERE p.id IN :ids AND a.userId <> :observer AND (
                a.visibility = com.app.APP.model.enums.AdventureVisibility.PUBLICA
                OR (a.visibility = com.app.APP.model.enums.AdventureVisibility.SO_GRUPO
                    AND EXISTS (SELECT 1 FROM AdventureParticipant part
                                WHERE part.adventure.id = a.id AND part.userId = :observer))
            )
            """)
    List<Path> findDiscoverableByIds(List<String> ids, String observer);
}
