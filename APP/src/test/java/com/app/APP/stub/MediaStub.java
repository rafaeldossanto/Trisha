package com.app.APP.stub;

import com.app.APP.entity.Media;
import com.app.APP.model.dto.request.MediaRequest;
import com.app.APP.model.enums.MediaType;

import java.time.LocalDateTime;

/**
 * Test helper for Media (metadata — APP does not store binaries).
 */
public final class MediaStub {

    public static final String ID = "midia-1";
    public static final String USER_ID = "usuario-1";
    public static final String URL = "https://cdn/midia.jpg";

    private MediaStub() {
    }

    public static Media.MediaBuilder aMedia() {
        return Media.builder()
                .id(ID)
                .adventure(AdventureStub.anAdventure().build())
                .path(PathStub.aPath().build())
                .userId(USER_ID)
                .type(MediaType.FOTO)
                .url(URL)
                .captureLat(-20.43)
                .captureLng(-41.79)
                .captureDistanceKm(1.5)
                .pathPercentage(0.30)
                .capturedAt(LocalDateTime.now());
    }

    /** Request with associated path. */
    public static MediaRequest aRequest() {
        return new MediaRequest(
                AdventureStub.ID, PathStub.ID, MediaType.FOTO,
                URL, -20.43, -41.79, 1.5, 0.30);
    }

    /** Request without path (standalone media). */
    public static MediaRequest aRequestWithoutPath() {
        return new MediaRequest(
                AdventureStub.ID, null, MediaType.FOTO,
                URL, -20.43, -41.79, 1.5, 0.30);
    }
}
