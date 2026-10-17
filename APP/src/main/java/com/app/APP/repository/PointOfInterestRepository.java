package com.app.APP.repository;

import com.app.APP.entity.PointOfInterest;
import com.app.APP.model.enums.PointType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, String> {
    Page<PointOfInterest> findByPathId(String pathId, Pageable pageable);
    List<PointOfInterest> findByPathIdAndType(String pathId, PointType type);

    /** Pontos de um conjunto de caminhos — usado na cascata ao descartar dados. */
    List<PointOfInterest> findByPathIdIn(List<String> pathIds);
}
