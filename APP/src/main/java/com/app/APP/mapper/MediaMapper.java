package com.app.APP.mapper;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Media;
import com.app.APP.entity.Path;
import com.app.APP.model.dto.request.MediaRequest;
import com.app.APP.model.dto.response.MediaResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

import static com.app.APP.mapper.MapperUtils.idOrNull;

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
