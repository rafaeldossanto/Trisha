package com.trisha.app.social;

import com.trisha.app.social.dto.CountersResponse;
import com.trisha.app.social.dto.FollowStatusResponse;
import com.trisha.app.user.User;
import com.trisha.app.user.UserRepository;
import com.trisha.app.user.dto.PublicUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Following is directional and requires no acceptance. The target is identified
 * by userCode (public handle, same as friendship); the service resolves to internal id.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FollowerService {

    private final FollowerRepository followerRepository;
    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;

    @Transactional
    public void follow(String followerId, String followedCode) {
        String followedId = resolveId(followedCode);
        if (followerId.equals(followedId)) {
            throw new IllegalArgumentException("Voce nao pode seguir a si mesmo");
        }
        if (isBlocked(followerId, followedId)) {
            throw new IllegalArgumentException("Nao e possivel seguir: ha um bloqueio entre voces");
        }
        if (followerRepository.existsByFollowerIdAndFollowedId(followerId, followedId)) {
            return;
        }
        followerRepository.save(Follower.builder()
                .id(UUID.randomUUID().toString())
                .followerId(followerId)
                .followedId(followedId)
                .build());
        log.info("{} started following {}", followerId, followedId);
    }

    @Transactional
    public void unfollow(String followerId, String followedCode) {
        followerRepository.deleteByFollowerIdAndFollowedId(followerId, resolveId(followedCode));
        log.info("{} unfollowed {}", followerId, followedCode);
    }

    @Transactional(readOnly = true)
    public Page<PublicUserResponse> getFollowers(String userCode, Pageable pageable) {
        return followerRepository.findFollowers(resolveId(userCode), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PublicUserResponse> getFollowing(String userCode, Pageable pageable) {
        return followerRepository.findFollowing(resolveId(userCode), pageable);
    }

    @Transactional(readOnly = true)
    public CountersResponse counters(String userCode) {
        String userId = resolveId(userCode);
        return new CountersResponse(
                followerRepository.countByFollowedId(userId),
                followerRepository.countByFollowerId(userId));
    }

    @Transactional(readOnly = true)
    public FollowStatusResponse status(String myId, String otherCode) {
        String otherId = resolveId(otherCode);
        boolean following = followerRepository.existsByFollowerIdAndFollowedId(myId, otherId);
        boolean followsMe = followerRepository.existsByFollowerIdAndFollowedId(otherId, myId);
        return new FollowStatusResponse(following, followsMe, following && followsMe);
    }

    @Transactional(readOnly = true)
    public boolean isFollower(String followerId, String followedId) {
        return followerRepository.existsByFollowerIdAndFollowedId(followerId, followedId);
    }

    @Transactional(readOnly = true)
    public List<String> followingIds(String userId) {
        return followerRepository.findFollowedIds(userId);
    }

    private String resolveId(String userCode) {
        return userRepository.findByUserCode(userCode)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario nao encontrado"));
    }

    private boolean isBlocked(String a, String b) {
        return friendshipRepository.findRelation(a, b)
                .filter(rel -> FriendshipStatus.BLOQUEADA.equals(rel.getStatus()))
                .isPresent();
    }
}
