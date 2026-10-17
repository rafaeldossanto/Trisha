package com.app.APP.repository;

import com.app.APP.entity.Friendship;
import com.app.APP.model.enums.FriendshipStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FriendshipRepository extends JpaRepository<Friendship, String> {

    /** Relations of a user (on either side) with a given status — e.g. friends (ACEITA). */
    @Query(value = "SELECT f FROM Friendship f WHERE (f.requesterId = :userId OR f.receiverId = :userId) AND f.status = :status",
            countQuery = "SELECT COUNT(f) FROM Friendship f WHERE (f.requesterId = :userId OR f.receiverId = :userId) AND f.status = :status")
    Page<Friendship> findByUserIdAndStatus(String userId, FriendshipStatus status, Pageable pageable);

    /** Requests that ARRIVED at the user (they are the receiver). */
    Page<Friendship> findByReceiverIdAndStatus(String receiverId, FriendshipStatus status, Pageable pageable);

    /** Requests the user SENT (they are the requester). */
    Page<Friendship> findByRequesterIdAndStatus(String requesterId, FriendshipStatus status, Pageable pageable);

    @Query("SELECT f FROM Friendship f WHERE (f.requesterId = :id1 AND f.receiverId = :id2) OR (f.requesterId = :id2 AND f.receiverId = :id1)")
    Optional<Friendship> findRelation(String id1, String id2);

    /** Ids of the user's friends (the other side of relations with a given status). */
    @Query("""
            SELECT CASE WHEN f.requesterId = :userId THEN f.receiverId ELSE f.requesterId END
            FROM Friendship f
            WHERE (f.requesterId = :userId OR f.receiverId = :userId) AND f.status = :status
            """)
    List<String> findFriendIds(String userId, FriendshipStatus status);
}
