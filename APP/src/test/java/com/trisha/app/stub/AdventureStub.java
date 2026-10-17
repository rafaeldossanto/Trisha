package com.trisha.app.stub;

import com.trisha.app.adventure.Adventure;
import com.trisha.app.adventure.AdventureStatus;
import com.trisha.app.adventure.AdventureVisibility;
import com.trisha.app.adventure.dto.AdventureRequest;
import com.trisha.app.region.Region;
import com.trisha.app.region.RegionVisibility;

import java.time.LocalDateTime;

/**
 * Test helper for Adventure.
 * Each method returns a builder already populated with valid values,
 * allowing direct use ({@code AdventureStub.anAdventure().build()})
 * or selective field override when the test requires it.
 */
public final class AdventureStub {

    public static final String ID = "aventura-1";
    public static final String USER_ID = "usuario-1";
    public static final String REGION_ID = "regiao-1";
    public static final String DESTINATION = "Pico da Bandeira";

    private AdventureStub() {
    }

    public static Adventure.AdventureBuilder anAdventure() {
        return Adventure.builder()
                .id(ID)
                .userId(USER_ID)
                .region(RegionStub.aRegion().build())
                .destination(DESTINATION)
                .status(AdventureStatus.PLANEJADA)
                .visibility(AdventureVisibility.PRIVADA)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now());
    }

    public static AdventureRequest aRequest() {
        return new AdventureRequest(REGION_ID, DESTINATION, AdventureVisibility.PRIVADA);
    }

    public static final class RegionStub {

        private RegionStub() {
        }

        public static Region.RegionBuilder aRegion() {
            return Region.builder()
                    .id(REGION_ID)
                    .userId(USER_ID)
                    .name("Serra do Caparao")
                    .description("Regiao de montanhas")
                    .visibility(RegionVisibility.PRIVADA);
        }
    }
}
