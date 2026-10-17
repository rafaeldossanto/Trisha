package com.app.APP.mapper;

import com.app.APP.entity.Friendship;
import com.app.APP.model.dto.response.FriendshipResponse;
import lombok.experimental.UtilityClass;

import java.time.LocalDateTime;
import java.util.UUID;

@UtilityClass
public class FriendshipMapper {

    public static Friendship toEntity(String requesterId, String receiverId) {
        return Friendship.builder()
                .id(UUID.randomUUID().toString())
                .requesterId(requesterId)
                .receiverId(receiverId)
                .requestedAt(LocalDateTime.now())
                .build();
    }

    public static FriendshipResponse toResponse(Friendship f) {
        return FriendshipResponse.builder()
                .id(f.getId())
                .requesterId(f.getRequesterId())
                .receiverId(f.getReceiverId())
                .status(f.getStatus())
                .blockedBy(f.getBlockedBy())
                .requestedAt(f.getRequestedAt())
                .respondedAt(f.getRespondedAt())
                .build();
    }
}
