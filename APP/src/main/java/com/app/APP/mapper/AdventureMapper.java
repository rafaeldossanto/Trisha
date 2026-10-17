package com.app.APP.mapper;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Region;
import com.app.APP.model.dto.request.AdventureRequest;
import com.app.APP.model.dto.response.AdventureResponse;
import lombok.experimental.UtilityClass;

import java.util.UUID;

import static com.app.APP.mapper.MapperUtils.idOrNull;

@UtilityClass
public class AdventureMapper {

    public static Adventure toEntity(AdventureRequest request, Region region, String userId) {
        return Adventure.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .region(region)
                .destination(request.destination())
                .visibility(request.visibility())
                .build();
    }

    public static AdventureResponse toResponse(Adventure adventure, long participantsCount, Double durationHours) {
        return AdventureResponse.builder()
                .id(adventure.getId())
                .userId(adventure.getUserId())
                .regionId(idOrNull(adventure.getRegion(), Region::getId))
                .destination(adventure.getDestination())
                .status(adventure.getStatus())
                .visibility(adventure.getVisibility())
                .createdAt(adventure.getCreatedAt())
                .participantsCount((int) participantsCount)
                .durationHours(durationHours)
                .build();
    }
}
