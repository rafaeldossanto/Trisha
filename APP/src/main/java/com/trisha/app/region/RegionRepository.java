package com.trisha.app.region;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, String> {

    Page<Region> findByUserId(String userId, Pageable pageable);

    /**
     * Regions visible to the requester that are not theirs: PUBLICA from anyone,
     * or AMIGOS whose owner is in the friends list.
     */
    @Query("""
            SELECT r FROM Region r
            WHERE r.userId <> :userId AND (
                r.visibility = com.trisha.app.region.RegionVisibility.PUBLICA
                OR (r.visibility = com.trisha.app.region.RegionVisibility.AMIGOS AND r.userId IN :friendIds)
            )
            """)
    Page<Region> findDiscoverable(String userId, List<String> friendIds, Pageable pageable);
}
