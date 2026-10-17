package com.trisha.app.path;

import com.trisha.app.adventure.Adventure;
import com.trisha.app.path.dto.PathDiscoveryResponse;
import com.trisha.app.path.dto.PathRequest;
import com.trisha.app.path.dto.PathResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class PathMapper {

    public static Path toEntity(PathRequest request, Adventure adventure, int number, String userId) {
        return Path.builder()
                .id(UUID.randomUUID().toString())
                .adventure(adventure)
                .userId(userId)
                .color(request.color())
                .number(number)
                .startedAt(LocalDateTime.now())
                .build();
    }

    public static PathDiscoveryResponse toDiscoveryResponse(Path path) {
        return PathDiscoveryResponse.builder()
                .id(path.getId())
                .adventureId(path.getAdventure().getId())
                .userId(path.getAdventure().getUserId())
                .destination(path.getAdventure().getDestination())
                .color(path.getColor())
                .build();
    }

    public static PathResponse toResponse(Path path) {
        return PathResponse.builder()
                .id(path.getId())
                .adventureId(path.getAdventure().getId())
                .userId(path.getUserId())
                .color(path.getColor())
                .number(path.getNumber())
                .startedAt(path.getStartedAt())
                .finishedAt(path.getFinishedAt())
                .totalDistanceKm(path.getTotalDistanceKm())
                .build();
    }
}
