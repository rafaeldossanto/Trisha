package com.trisha.app.point;

import com.trisha.app.point.dto.PointStatusResponse;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class PointOfInterestStatusMapper {

    public static PointOfInterestUserStatus toEntity(String userId, String pointId) {
        return PointOfInterestUserStatus.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .pointId(pointId)
                .build();
    }

    public static PointStatusResponse toResponse(PointOfInterestUserStatus status) {
        return PointStatusResponse.builder()
                .pointId(status.getPointId())
                .status(status.getStatus())
                .goal(status.isGoal())
                .build();
    }
}
