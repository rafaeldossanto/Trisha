package com.app.APP.repository;

import com.app.APP.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EvidenceRepository extends JpaRepository<Evidence, String> {
    List<Evidence> findByPointId(String pointId);

    @Query("SELECT COUNT(DISTINCT e.userId) FROM Evidence e WHERE e.point.id = :pointId AND e.validated = true")
    Long countValidatedUsersByPointId(String pointId);

    /**
     * Counts distinct validated users for MULTIPLE points at once, avoiding
     * N+1 when listing points of a path. Returns [pointId, total] per row.
     */
    @Query("SELECT e.point.id, COUNT(DISTINCT e.userId) FROM Evidence e " +
            "WHERE e.point.id IN :pointIds AND e.validated = true GROUP BY e.point.id")
    List<Object[]> countValidatedUsersPerPoint(List<String> pointIds);

    boolean existsByPointIdAndUserId(String pointId, String userId);

    /** Remove evidencias de um conjunto de pontos — cascata ao descartar dados. */
    void deleteByPointIdIn(List<String> pointIds);
}
