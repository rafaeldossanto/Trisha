package com.app.APP.repository;

import com.app.APP.entity.AdventureParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AdventureParticipantRepository extends JpaRepository<AdventureParticipant, String> {
    boolean existsByAdventureIdAndUserId(String adventureId, String userId);

    long countByAdventureId(String adventureId);

    List<AdventureParticipant> findByAdventureId(String adventureId);

    void deleteByAdventureIdAndUserId(String adventureId, String userId);

    void deleteByAdventureId(String adventureId);

    /** Contagem de participantes por aventura em lote — evita N+1 ao enriquecer listas. */
    @Query("SELECT p.adventure.id, COUNT(p) FROM AdventureParticipant p WHERE p.adventure.id IN :ids GROUP BY p.adventure.id")
    List<Object[]> countByAdventureIds(List<String> ids);
}
