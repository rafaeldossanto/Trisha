package com.app.APP.stub;

import com.app.APP.entity.Evidence;
import com.app.APP.entity.PointOfInterest;
import com.app.APP.model.dto.request.EvidenceRequest;
import com.app.APP.model.dto.request.PointOfInterestRequest;
import com.app.APP.model.enums.EvidenceType;
import com.app.APP.model.enums.PointType;

import java.time.LocalDateTime;

/**
 * Test helper for PointOfInterest and Evidence.
 * Default capture coordinates match the point's coordinates
 * (distance 0m), so the default evidence always passes the
 * proximity validation (&lt; 50m).
 */
public final class PointOfInterestStub {

    public static final String ID = "ponto-1";
    public static final String USER_ID = "usuario-1";
    public static final double LATITUDE = -20.4350;
    public static final double LONGITUDE = -41.7920;

    private PointOfInterestStub() {
    }

    public static PointOfInterest.PointOfInterestBuilder aPoint() {
        return PointOfInterest.builder()
                .id(ID)
                .path(PathStub.aPath().build())
                .userId(USER_ID)
                .type(PointType.MIRANTE)
                .name("Mirante do Vale")
                .description("Vista panoramica")
                .latitude(LATITUDE)
                .longitude(LONGITUDE)
                .createdAt(LocalDateTime.now());
    }

    public static PointOfInterestRequest aRequest() {
        return new PointOfInterestRequest(
                PathStub.ID, PointType.MIRANTE,
                "Mirante do Vale", "Vista panoramica", LATITUDE, LONGITUDE);
    }

    public static Evidence.EvidenceBuilder anEvidence() {
        return Evidence.builder()
                .id("evidencia-1")
                .point(aPoint().build())
                .userId(USER_ID)
                .photoUrl("https://cdn/foto.jpg")
                .evidenceType(EvidenceType.VISTA)
                .captureLat(LATITUDE)
                .captureLng(LONGITUDE)
                .distanceFromPointM(0.0)
                .capturedInApp(true)
                .validated(true)
                .createdAt(LocalDateTime.now());
    }

    /** Evidence captured exactly on the point (distance ~0m, valid). */
    public static EvidenceRequest aCloseEvidenceRequest() {
        return new EvidenceRequest(
                ID, "https://cdn/foto.jpg",
                EvidenceType.VISTA, LATITUDE, LONGITUDE);
    }

    /** Evidence captured far from the point (~111km, should be rejected). */
    public static EvidenceRequest aFarEvidenceRequest() {
        return new EvidenceRequest(
                ID, "https://cdn/foto.jpg",
                EvidenceType.VISTA, LATITUDE + 1.0, LONGITUDE);
    }
}
