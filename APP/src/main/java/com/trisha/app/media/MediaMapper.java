package com.trisha.app.media;

import com.trisha.app.adventure.Adventure;
import com.trisha.app.media.dto.MediaRequest;
import com.trisha.app.media.dto.MediaResponse;
import com.trisha.app.path.Path;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.trisha.app.shared.util.MapperUtils.idOrNull;

@UtilityClass
public class MediaMapper {

    public static Media toEntity(MediaRequest request, Adventure adventure, Path path, String userId) {
        return Media.builder()
                .id(UUID.randomUUID().toString())
                .adventure(adventure)
                .path(path)
                .userId(userId)
                .type(request.type())
                .url(request.url())
                .captureLat(request.captureLat())
                .captureLng(request.captureLng())
                .captureDistanceKm(request.captureDistanceKm())
                .pathPercentage(request.pathPercentage())
                .capturedAt(LocalDateTime.now())
                .build();
    }

    public static MediaResponse toResponse(Media media) {
        return MediaResponse.builder()
                .id(media.getId())
                .adventureId(media.getAdventure().getId())
                .pathId(idOrNull(media.getPath(), Path::getId))
                .type(media.getType())
                .url(media.getUrl())
                .pathPercentage(media.getPathPercentage())
                .captureDistanceKm(media.getCaptureDistanceKm())
                .capturedAt(media.getCapturedAt())
                .build();
    }
}
