package com.app.APP.repository;

import com.app.APP.entity.Adventure;
import com.app.APP.model.enums.AdventureStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AdventureRepository extends JpaRepository<Adventure, String> {
    Page<Adventure> findByUserId(String userId, Pageable pageable);
    List<Adventure> findByUserIdAndStatus(String userId, AdventureStatus status);

    /**
     * Carrega a aventura com lock de escrita (SELECT ... FOR UPDATE) para
     * serializar o inicio de caminhos na mesma aventura: o numero do caminho vem
     * de {@code countByAdventureId()+1} e varios participantes podem iniciar ao
     * mesmo tempo, o que sem lock geraria numero duplicado.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Adventure a WHERE a.id = :id")
    Optional<Adventure> findByIdForUpdate(@Param("id") String id);

    /** When deleting a region (folder), adventures are not deleted — only unlinked. */
    @Modifying
    @Query("UPDATE Adventure a SET a.region = null WHERE a.region.id = :regionId")
    void unlinkRegion(String regionId);

    /**
     * Adventures in a region visible to the observer: PUBLICA for everyone,
     * own adventures always, SO_GRUPO only if they participate.
     * The folder never exposes an adventure the observer would not see on their own.
     */
    @Query("""
            SELECT a FROM Adventure a
            WHERE a.region.id = :regionId AND (
                a.visibility = com.app.APP.model.enums.AdventureVisibility.PUBLICA
                OR a.userId = :observer
                OR (a.visibility = com.app.APP.model.enums.AdventureVisibility.SO_GRUPO
                    AND EXISTS (SELECT 1 FROM AdventureParticipant p WHERE p.adventure.id = a.id AND p.userId = :observer))
            )
            """)
    Page<Adventure> findVisibleInRegion(String regionId, String observer, Pageable pageable);

    /** Someone else's adventures the observer may see (owner uses findByUserId). */
    @Query("""
            SELECT a FROM Adventure a
            WHERE a.userId = :owner AND (
                a.visibility = com.app.APP.model.enums.AdventureVisibility.PUBLICA
                OR (a.visibility = com.app.APP.model.enums.AdventureVisibility.SO_GRUPO
                    AND EXISTS (SELECT 1 FROM AdventureParticipant p WHERE p.adventure.id = a.id AND p.userId = :observer))
            )
            """)
    Page<Adventure> findVisibleByUser(String owner, String observer, Pageable pageable);

    /**
     * Feed: the observer's own adventures plus the visible ones from the users
     * they follow, newest first. `authors` never comes empty (sentinel).
     */
    @Query("""
            SELECT a FROM Adventure a
            WHERE (a.userId = :observer OR (a.userId IN :authors AND (
                a.visibility = com.app.APP.model.enums.AdventureVisibility.PUBLICA
                OR (a.visibility = com.app.APP.model.enums.AdventureVisibility.SO_GRUPO
                    AND EXISTS (SELECT 1 FROM AdventureParticipant p WHERE p.adventure.id = a.id AND p.userId = :observer))
            )))
            ORDER BY a.createdAt DESC
            """)
    Page<Adventure> findFeed(String observer, List<String> authors, Pageable pageable);
}
