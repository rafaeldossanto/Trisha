package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.response.CountersResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.FollowStatusResponse;
import com.trisha.bff.model.dto.response.PublicUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/** Orquestra seguir/deixar de seguir, listas, contadores e status sobre o APP. */
@Service
@RequiredArgsConstructor
@Slf4j
public class FollowerBffService {

    private final AppClient appClient;

    public void follow(String followedId) {
        log.info("BFF: seguir {}", followedId);
        appClient.follow(followedId);
    }

    public void unfollow(String followedId) {
        log.info("BFF: deixar de seguir {}", followedId);
        appClient.unfollow(followedId);
    }

    public PageResponse<PublicUserResponse> followers(String userId, Pageable pageable) {
        return appClient.getFollowers(userId, pageable);
    }

    public PageResponse<PublicUserResponse> following(String userId, Pageable pageable) {
        return appClient.getFollowing(userId, pageable);
    }

    public CountersResponse counters(String userId) {
        return appClient.getCounters(userId);
    }

    public FollowStatusResponse status(String userId) {
        return appClient.getFollowStatus(userId);
    }
}
