package com.app.APP.service;

import com.app.APP.entity.Friendship;
import com.app.APP.entity.User;
import com.app.APP.model.dto.request.FriendshipRequest;
import com.app.APP.model.dto.response.FriendshipResponse;
import com.app.APP.model.enums.FriendshipStatus;
import com.app.APP.repository.FriendshipRepository;
import com.app.APP.repository.FollowerRepository;
import com.app.APP.repository.UserRepository;
import com.app.APP.stub.FriendshipStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendshipService")
class FriendshipServiceTest {

    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FollowerRepository followerRepository;

    @InjectMocks
    private FriendshipService service;

    private final Pageable pageable = PageRequest.of(0, 10);

    /** Receiver resolved by userCode (User entity is @Immutable). */
    private User receiver() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(FriendshipStub.RECEIVER_ID);
        return user;
    }

    // ---- request ----

    @Test
    @DisplayName("request should create pending friendship with the token requester")
    void shouldRequestFriendship() {
        FriendshipRequest request = FriendshipStub.aRequest();
        final User receiver = receiver();
        when(userRepository.findByUserCode(FriendshipStub.RECEIVER_CODE)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findRelation(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID)).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));
        when(followerRepository.existsByFollowerIdAndFollowedId(any(), any())).thenReturn(true);

        FriendshipResponse response = service.request(FriendshipStub.REQUESTER_ID, request);

        assertThat(response.requesterId()).isEqualTo(FriendshipStub.REQUESTER_ID);
        assertThat(response.receiverId()).isEqualTo(FriendshipStub.RECEIVER_ID);
        assertThat(response.status()).isEqualTo(FriendshipStatus.PENDENTE);
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    @DisplayName("request should fail when receiver does not exist")
    void shouldFailReceiverNotFound() {
        when(userRepository.findByUserCode(FriendshipStub.RECEIVER_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(FriendshipStub.REQUESTER_ID, FriendshipStub.aRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("receptor nao encontrado");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("request should fail when relation already exists")
    void shouldFailDuplicateRelation() {
        final User receiver = receiver();
        when(userRepository.findByUserCode(FriendshipStub.RECEIVER_CODE)).thenReturn(Optional.of(receiver));
        when(followerRepository.existsByFollowerIdAndFollowedId(any(), any())).thenReturn(true);
        when(friendshipRepository.findRelation(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID))
                .thenReturn(Optional.of(FriendshipStub.aFriendship().build()));

        assertThatThrownBy(() -> service.request(FriendshipStub.REQUESTER_ID, FriendshipStub.aRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ja existe uma relacao");

        verify(friendshipRepository, never()).save(any());
    }

    // ---- respond ----

    @Test
    @DisplayName("respond should accept when it is the receiver")
    void shouldRespondAccepted() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.PENDENTE).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendshipResponse response = service.respond(FriendshipStub.RECEIVER_ID, FriendshipStub.ID, FriendshipStatus.ACEITA);

        assertThat(response.status()).isEqualTo(FriendshipStatus.ACEITA);
        assertThat(friendship.getRespondedAt()).isNotNull();
    }

    @Test
    @DisplayName("respond should fail when not the receiver")
    void shouldFailRespondNotReceiver() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.PENDENTE).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> service.respond(FriendshipStub.REQUESTER_ID, FriendshipStub.ID, FriendshipStatus.ACEITA))
                .isInstanceOf(com.app.APP.exception.ForbiddenException.class)
                .hasMessageContaining("destinatario");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("respond should fail when already responded")
    void shouldFailRespondAlreadyAnswered() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.ACEITA).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> service.respond(FriendshipStub.RECEIVER_ID, FriendshipStub.ID, FriendshipStatus.RECUSADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja foi respondida");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("respond should only accept ACEITA or RECUSADA")
    void shouldFailRespondInvalidStatus() {
        // BLOQUEADA via respond criaria um bloqueio sem blockedBy, que ninguem
        // conseguiria desbloquear; bloquear tem endpoint proprio.
        assertThatThrownBy(() -> service.respond(FriendshipStub.RECEIVER_ID, FriendshipStub.ID, FriendshipStatus.BLOQUEADA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resposta invalida");

        assertThatThrownBy(() -> service.respond(FriendshipStub.RECEIVER_ID, FriendshipStub.ID, FriendshipStatus.PENDENTE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Resposta invalida");

        verify(friendshipRepository, never()).save(any());
    }

    // ---- cancelRequest / unfriend ----

    @Test
    @DisplayName("cancelRequest should remove when it is the requester")
    void shouldCancelRequest() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.PENDENTE).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        service.cancelRequest(FriendshipStub.REQUESTER_ID, FriendshipStub.ID);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    @DisplayName("cancelRequest should fail when not the requester")
    void shouldFailCancelNotRequester() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.PENDENTE).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> service.cancelRequest(FriendshipStub.RECEIVER_ID, FriendshipStub.ID))
                .isInstanceOf(com.app.APP.exception.ForbiddenException.class)
                .hasMessageContaining("quem enviou");

        verify(friendshipRepository, never()).delete(any());
    }

    @Test
    @DisplayName("unfriend should remove when participant and accepted")
    void shouldUnfriend() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.ACEITA).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        service.unfriend(FriendshipStub.RECEIVER_ID, FriendshipStub.ID);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    @DisplayName("unfriend should fail when not a participant")
    void shouldFailUnfriendNotParticipant() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.ACEITA).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> service.unfriend("stranger", FriendshipStub.ID))
                .isInstanceOf(com.app.APP.exception.ForbiddenException.class)
                .hasMessageContaining("nao participa");

        verify(friendshipRepository, never()).delete(any());
    }

    // ---- block / unblock ----

    @Test
    @DisplayName("block should mark existing relation as BLOQUEADA and record who blocked")
    void shouldBlockExistingRelation() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.ACEITA).build();
        final User receiver = receiver();
        when(userRepository.findByUserCode(FriendshipStub.RECEIVER_CODE)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findRelation(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID)).thenReturn(Optional.of(friendship));
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendshipResponse response = service.block(FriendshipStub.REQUESTER_ID, FriendshipStub.aRequest());

        assertThat(response.status()).isEqualTo(FriendshipStatus.BLOQUEADA);
        assertThat(response.blockedBy()).isEqualTo(FriendshipStub.REQUESTER_ID);
        // O bloqueio corta o vinculo social: follows dos dois lados caem.
        verify(followerRepository).deleteByFollowerIdAndFollowedId(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID);
        verify(followerRepository).deleteByFollowerIdAndFollowedId(FriendshipStub.RECEIVER_ID, FriendshipStub.REQUESTER_ID);
    }

    @Test
    @DisplayName("block should create BLOQUEADA relation when there was no prior relation")
    void shouldBlockWithoutPriorRelation() {
        final User receiver = receiver();
        when(userRepository.findByUserCode(FriendshipStub.RECEIVER_CODE)).thenReturn(Optional.of(receiver));
        when(friendshipRepository.findRelation(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID)).thenReturn(Optional.empty());
        when(friendshipRepository.save(any(Friendship.class))).thenAnswer(inv -> inv.getArgument(0));

        FriendshipResponse response = service.block(FriendshipStub.REQUESTER_ID, FriendshipStub.aRequest());

        assertThat(response.status()).isEqualTo(FriendshipStatus.BLOQUEADA);
        verify(friendshipRepository).save(any(Friendship.class));
    }

    @Test
    @DisplayName("block should fail when target does not exist")
    void shouldFailBlockTargetNotFound() {
        when(userRepository.findByUserCode(FriendshipStub.RECEIVER_CODE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.block(FriendshipStub.REQUESTER_ID, FriendshipStub.aRequest()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");

        verify(friendshipRepository, never()).save(any());
    }

    @Test
    @DisplayName("unblock should remove when it is the one who blocked")
    void shouldUnblock() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.BLOQUEADA).blockedBy(FriendshipStub.REQUESTER_ID).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        service.unblock(FriendshipStub.REQUESTER_ID, FriendshipStub.ID);

        verify(friendshipRepository).delete(friendship);
    }

    @Test
    @DisplayName("unblock should fail when not the one who blocked")
    void shouldFailUnblockNotBlocker() {
        Friendship friendship = FriendshipStub.aFriendship().status(FriendshipStatus.BLOQUEADA).blockedBy(FriendshipStub.REQUESTER_ID).build();
        when(friendshipRepository.findById(FriendshipStub.ID)).thenReturn(Optional.of(friendship));

        assertThatThrownBy(() -> service.unblock(FriendshipStub.RECEIVER_ID, FriendshipStub.ID))
                .isInstanceOf(com.app.APP.exception.ForbiddenException.class)
                .hasMessageContaining("quem bloqueou");

        verify(friendshipRepository, never()).delete(any());
    }

    // ---- paginated listings ----

    @Test
    @DisplayName("getPending returns received pending requests")
    void shouldListReceivedPending() {
        when(friendshipRepository.findByReceiverIdAndStatus(FriendshipStub.RECEIVER_ID, FriendshipStatus.PENDENTE, pageable))
                .thenReturn(new PageImpl<>(List.of(FriendshipStub.aFriendship().build())));

        Page<FriendshipResponse> response = service.getPending(FriendshipStub.RECEIVER_ID, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getSent returns sent pending requests")
    void shouldListSentPending() {
        when(friendshipRepository.findByRequesterIdAndStatus(FriendshipStub.REQUESTER_ID, FriendshipStatus.PENDENTE, pageable))
                .thenReturn(new PageImpl<>(List.of(FriendshipStub.aFriendship().build())));

        Page<FriendshipResponse> response = service.getSent(FriendshipStub.REQUESTER_ID, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getFriends filters by ACEITA")
    void shouldListFriends() {
        when(friendshipRepository.findByUserIdAndStatus(FriendshipStub.REQUESTER_ID, FriendshipStatus.ACEITA, pageable))
                .thenReturn(new PageImpl<>(List.of(FriendshipStub.aFriendship().status(FriendshipStatus.ACEITA).build())));

        Page<FriendshipResponse> response = service.getFriends(FriendshipStub.REQUESTER_ID, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).status()).isEqualTo(FriendshipStatus.ACEITA);
    }

    // ---- areFriends ----

    @Test
    @DisplayName("areFriends should be true when there is an ACEITA relation")
    void shouldReturnTrueAreFriends() {
        when(friendshipRepository.findRelation(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID))
                .thenReturn(Optional.of(FriendshipStub.aFriendship().status(FriendshipStatus.ACEITA).build()));

        assertThat(service.areFriends(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID)).isTrue();
    }

    @Test
    @DisplayName("areFriends should be false when there is no accepted relation")
    void shouldReturnFalseNotFriends() {
        when(friendshipRepository.findRelation(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID)).thenReturn(Optional.empty());

        assertThat(service.areFriends(FriendshipStub.REQUESTER_ID, FriendshipStub.RECEIVER_ID)).isFalse();
    }

    // ---- friendIds ----

    @Test
    @DisplayName("friendIds should return only ACEITA relations ids")
    void shouldListFriendIds() {
        when(friendshipRepository.findFriendIds(FriendshipStub.REQUESTER_ID, FriendshipStatus.ACEITA))
                .thenReturn(List.of("usuario-2", "usuario-3"));

        List<String> ids = service.friendIds(FriendshipStub.REQUESTER_ID);

        assertThat(ids).containsExactly("usuario-2", "usuario-3");
    }
}
