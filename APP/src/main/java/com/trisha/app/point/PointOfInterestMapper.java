package com.trisha.app.point;

import com.trisha.app.path.Path;
import com.trisha.app.point.dto.EvidenceRequest;
import com.trisha.app.point.dto.EvidenceResponse;
import com.trisha.app.point.dto.PointOfInterestRequest;
import com.trisha.app.point.dto.PointOfInterestResponse;
import lombok.experimental.UtilityClass;

import java.util.UUID;

@UtilityClass
public class PointOfInterestMapper {

    public static PointOfInterest toEntity(PointOfInterestRequest request, Path path, String userId) {
        return PointOfInterest.builder()
                .id(UUID.randomUUID().toString())
                .path(path)
                .userId(userId)
                .type(request.type())
                .name(request.name())
                .description(request.description())
                .latitude(request.latitude())
                .longitude(request.longitude())
                .build();
    }

    public static Evidence toEvidenceEntity(EvidenceRequest request, PointOfInterest point, double distance, String userId) {
        return Evidence.builder()
                .id(UUID.randomUUID().toString())
                .point(point)
                .userId(userId)
                .photoUrl(request.photoUrl())
                .evidenceType(request.evidenceType())
                .captureLat(request.captureLat())
                .captureLng(request.captureLng())
                .distanceFromPointM(distance)
                .capturedInApp(true)
                .validated(true)
                .build();
    }

    public static PointOfInterestResponse toResponse(PointOfInterest point, int confidenceLevel) {
        return PointOfInterestResponse.builder()
                .id(point.getId())
                .pathId(point.getPath().getId())
                .userId(point.getUserId())
                .type(point.getType())
                .name(point.getName())
                .description(point.getDescription())
                .latitude(point.getLatitude())
                .longitude(point.getLongitude())
                .confidenceLevel(confidenceLevel)
                .createdAt(point.getCreatedAt())
                .build();
    }

    public static EvidenceResponse toEvidenceResponse(Evidence evidence) {
        return EvidenceResponse.builder()
                .id(evidence.getId())
                .pointId(evidence.getPoint().getId())
                .userId(evidence.getUserId())
                .photoUrl(evidence.getPhotoUrl())
                .evidenceType(evidence.getEvidenceType())
                .validated(evidence.getValidated())
                .createdAt(evidence.getCreatedAt())
                .build();
    }
}
