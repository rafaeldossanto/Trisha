package com.app.APP.mapper;

import com.app.APP.entity.City;
import com.app.APP.entity.Region;
import com.app.APP.model.dto.request.CityDTO;
import com.app.APP.model.dto.request.RegionRequest;
import com.app.APP.model.dto.response.RegionResponse;
import com.app.APP.model.enums.RegionVisibility;
import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@UtilityClass
public class RegionMapper {

    public static Region toEntity(RegionRequest request, String userId) {
        return Region.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .name(request.name())
                .description(request.description())
                .coverUrl(request.coverUrl())
                .visibility(visibilityOrDefault(request))
                .cities(toCities(request.cities()))
                .build();
    }

    /** Applies editable fields to an existing region (PUT). */
    public static void apply(Region region, RegionRequest request) {
        region.setName(request.name());
        region.setDescription(request.description());
        region.setCoverUrl(request.coverUrl());
        region.setVisibility(visibilityOrDefault(request));
        region.setCities(toCities(request.cities()));
    }

    public static RegionResponse toResponse(Region region) {
        return RegionResponse.builder()
                .id(region.getId())
                .userId(region.getUserId())
                .name(region.getName())
                .description(region.getDescription())
                .coverUrl(region.getCoverUrl())
                .visibility(region.getVisibility())
                .cities(region.getCities().stream()
                        .map(c -> new CityDTO(c.getName(), c.getLatitude(), c.getLongitude(), c.getAltitude()))
                        .toList())
                .createdAt(region.getCreatedAt())
                .build();
    }

    private static RegionVisibility visibilityOrDefault(RegionRequest request) {
        return isNull(request.visibility()) ? RegionVisibility.PRIVADA : request.visibility();
    }

    private static List<City> toCities(List<CityDTO> cities) {
        if (isNull(cities)) {
            return new ArrayList<>();
        }
        return cities.stream()
                .map(c -> City.builder()
                        .name(c.name())
                        .latitude(c.latitude())
                        .longitude(c.longitude())
                        .altitude(c.altitude())
                        .build())
                .collect(Collectors.toCollection(ArrayList::new));
    }
}
