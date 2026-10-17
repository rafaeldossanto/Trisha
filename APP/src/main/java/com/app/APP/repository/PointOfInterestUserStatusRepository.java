package com.app.APP.repository;

import com.app.APP.entity.PointOfInterestUserStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PointOfInterestUserStatusRepository extends JpaRepository<PointOfInterestUserStatus, String> {

    Optional<PointOfInterestUserStatus> findByUserIdAndPointId(String userId, String pointId);

    List<PointOfInterestUserStatus> findByUserIdAndPointIdIn(String userId, List<String> pointIds);

    /** Remove marcacoes pessoais de um conjunto de pontos — cascata ao descartar dados. */
    void deleteByPointIdIn(List<String> pointIds);
}
