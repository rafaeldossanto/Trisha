package com.app.APP.repository;

import com.app.APP.entity.Follower;
import com.app.APP.model.dto.response.PublicUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface FollowerRepository extends JpaRepository<Follower, String> {

    boolean existsByFollowerIdAndFollowedId(String followerId, String followedId);

    void deleteByFollowerIdAndFollowedId(String followerId, String followedId);

    /** How many users follow this user. */
    long countByFollowedId(String followedId);

    /** How many users this user follows. */
    long countByFollowerId(String followerId);

    /** Who follows this user (public view). */
    @Query(value = "SELECT new com.app.APP.model.dto.response.PublicUserResponse(u.userCode, u.name) "
            + "FROM Follower f, User u WHERE u.id = f.followerId AND f.followedId = :userId",
            countQuery = "SELECT COUNT(f) FROM Follower f WHERE f.followedId = :userId")
    Page<PublicUserResponse> findFollowers(String userId, Pageable pageable);

    /** Who this user follows (public view). */
    @Query(value = "SELECT new com.app.APP.model.dto.response.PublicUserResponse(u.userCode, u.name) "
            + "FROM Follower f, User u WHERE u.id = f.followedId AND f.followerId = :userId",
            countQuery = "SELECT COUNT(f) FROM Follower f WHERE f.followerId = :userId")
    Page<PublicUserResponse> findFollowing(String userId, Pageable pageable);

    /** Ids of everyone this user follows — the feed's author list. */
    @Query("SELECT f.followedId FROM Follower f WHERE f.followerId = :userId")
    List<String> findFollowedIds(String userId);
}
