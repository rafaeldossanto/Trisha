package com.app.APP.service;

import com.app.APP.entity.Friendship;
import com.app.APP.entity.User;
import com.app.APP.exception.ForbiddenException;
import com.app.APP.mapper.FriendshipMapper;
import com.app.APP.model.dto.request.FriendshipRequest;
import com.app.APP.model.dto.response.FriendshipResponse;
import com.app.APP.model.enums.FriendshipStatus;
import com.app.APP.repository.FriendshipRepository;
import com.app.APP.repository.FollowerRepository;
import com.app.APP.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static com.app.APP.mapper.FriendshipMapper.toResponse;


@Service
@RequiredArgsConstructor
@Slf4j
public class FriendshipService {

    private final FriendshipRepository friendshipRepository;
    private final UserRepository userRepository;
    private final FollowerRepository followerRepository;

    public FriendshipResponse request(String requesterId, FriendshipRequest request) {
        log.info("Friendship request from {} to {}", requesterId, request.receiverCode());

        String receiverId = resolveReceiver(request.receiverCode());

        if (!followEachOther(requesterId, receiverId)) {
            throw new IllegalArgumentException("Voces precisam se seguir mutuamente para virar amigos");
        }

        friendshipRepository.findRelation(requesterId, receiverId)
                .ifPresent(f -> { throw new IllegalArgumentException("Ja existe uma relacao entre esses usuarios"); });

        return toResponse(friendshipRepository.save(FriendshipMapper.toEntity(requesterId, receiverId)));
    }

    public FriendshipResponse respond(String userId, String friendshipId, FriendshipStatus status) {
        if (!FriendshipStatus.ACEITA.equals(status) && !FriendshipStatus.RECUSADA.equals(status)) {
            throw new IllegalArgumentException("Resposta invalida: aceite ou recuse a solicitacao");
        }

        Friendship friendship = findById(friendshipId);

        if (!userId.equals(friendship.getReceiverId())) {
            throw new ForbiddenException("Apenas o destinatario pode responder a solicitacao");
        }
        if (!FriendshipStatus.PENDENTE.equals(friendship.getStatus())) {
            throw new IllegalArgumentException("Essa solicitacao ja foi respondida");
        }

        friendship.setStatus(status);
        friendship.setRespondedAt(LocalDateTime.now());

        log.info("Friendship {} responded with status: {}", friendshipId, status);
        return toResponse(friendshipRepository.save(friendship));
    }

    public void cancelRequest(String userId, String friendshipId) {
        Friendship friendship = findById(friendshipId);

        if (!userId.equals(friendship.getRequesterId())) {
            throw new ForbiddenException("Apenas quem enviou pode cancelar a solicitacao");
        }
        if (!FriendshipStatus.PENDENTE.equals(friendship.getStatus())) {
            throw new IllegalArgumentException("So e possivel cancelar uma solicitacao pendente");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship request {} cancelled", friendshipId);
    }

    public void unfriend(String userId, String friendshipId) {
        Friendship friendship = findById(friendshipId);
        validateParticipant(friendship, userId);

        if (!FriendshipStatus.ACEITA.equals(friendship.getStatus())) {
            throw new IllegalArgumentException("So e possivel desfazer uma amizade aceita");
        }

        friendshipRepository.delete(friendship);
        log.info("Friendship {} ended", friendshipId);
    }

    @Transactional
    public FriendshipResponse block(String blockerId, FriendshipRequest request) {
        log.info("User {} blocking {}", blockerId, request.receiverCode());

        String receiverId = resolveReceiver(request.receiverCode());

        Friendship friendship = friendshipRepository.findRelation(blockerId, receiverId)
                .orElseGet(() -> FriendshipMapper.toEntity(blockerId, receiverId));

        friendship.setStatus(FriendshipStatus.BLOQUEADA);
        friendship.setBlockedBy(blockerId);
        friendship.setRespondedAt(LocalDateTime.now());

        followerRepository.deleteByFollowerIdAndFollowedId(blockerId, receiverId);
        followerRepository.deleteByFollowerIdAndFollowedId(receiverId, blockerId);

        return toResponse(friendshipRepository.save(friendship));
    }

    public void unblock(String userId, String friendshipId) {
        Friendship friendship = findById(friendshipId);

        if (!FriendshipStatus.BLOQUEADA.equals(friendship.getStatus())) {
            throw new IllegalArgumentException("Essa relacao nao esta bloqueada");
        }
        if (!userId.equals(friendship.getBlockedBy())) {
            throw new ForbiddenException("Apenas quem bloqueou pode desbloquear");
        }

        friendshipRepository.delete(friendship);
        log.info("Block on relation {} removed", friendshipId);
    }

    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getPending(String userId, Pageable pageable) {
        return friendshipRepository.findByReceiverIdAndStatus(userId, FriendshipStatus.PENDENTE, pageable)
                .map(FriendshipMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getSent(String userId, Pageable pageable) {
        return friendshipRepository.findByRequesterIdAndStatus(userId, FriendshipStatus.PENDENTE, pageable)
                .map(FriendshipMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<FriendshipResponse> getFriends(String userId, Pageable pageable) {
        return friendshipRepository.findByUserIdAndStatus(userId, FriendshipStatus.ACEITA, pageable)
                .map(FriendshipMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public boolean areFriends(String userA, String userB) {
        return friendshipRepository.findRelation(userA, userB)
                .filter(f -> FriendshipStatus.ACEITA.equals(f.getStatus()))
                .isPresent();
    }

    @Transactional(readOnly = true)
    public List<String> friendIds(String userId) {
        return friendshipRepository.findFriendIds(userId, FriendshipStatus.ACEITA);
    }

    private boolean followEachOther(String a, String b) {
        return followerRepository.existsByFollowerIdAndFollowedId(a, b)
                && followerRepository.existsByFollowerIdAndFollowedId(b, a);
    }

    private String resolveReceiver(String userCode) {
        return userRepository.findByUserCode(userCode)
                .map(User::getId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario receptor nao encontrado"));
    }

    private Friendship findById(String id) {
        return friendshipRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Amizade nao encontrada"));
    }

    private void validateParticipant(Friendship friendship, String userId) {
        boolean participates = userId.equals(friendship.getRequesterId()) || userId.equals(friendship.getReceiverId());
        if (!participates) {
            throw new ForbiddenException("Voce nao participa dessa relacao");
        }
    }
}
