package com.app.APP.mapper;

import com.app.APP.entity.PointOfInterestUserStatus;
import com.app.APP.model.dto.response.PointStatusResponse;
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
