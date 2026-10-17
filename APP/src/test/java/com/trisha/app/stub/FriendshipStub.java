package com.trisha.app.stub;

import com.trisha.app.social.Friendship;
import com.trisha.app.social.FriendshipStatus;
import com.trisha.app.social.dto.FriendshipRequest;

import java.time.LocalDateTime;

/**
 * Test helper for Friendship.
 */
public final class FriendshipStub {

    public static final String ID = "amizade-1";
    public static final String REQUESTER_ID = "usuario-1";
    public static final String RECEIVER_ID = "usuario-2";
    public static final String RECEIVER_CODE = "rafael#2";

    private FriendshipStub() {
    }

    public static Friendship.FriendshipBuilder aFriendship() {
        return Friendship.builder()
                .id(ID)
                .requesterId(REQUESTER_ID)
                .receiverId(RECEIVER_ID)
                .status(FriendshipStatus.PENDENTE)
                .requestedAt(LocalDateTime.now());
    }

    public static FriendshipRequest aRequest() {
        return new FriendshipRequest(RECEIVER_CODE);
    }
}
