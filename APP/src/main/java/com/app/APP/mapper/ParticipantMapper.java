package com.app.APP.mapper;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.AdventureParticipant;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class ParticipantMapper {

    public static AdventureParticipant toEntity(Adventure adventure, String userId) {
        return AdventureParticipant.builder()
                .id(UUID.randomUUID().toString())
                .adventure(adventure)
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build();
    }
}
