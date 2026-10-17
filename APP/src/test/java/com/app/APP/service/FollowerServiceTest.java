package com.app.APP.service;

import com.app.APP.entity.Follower;
import com.app.APP.entity.User;
import com.app.APP.model.dto.response.CountersResponse;
import com.app.APP.model.dto.response.FollowStatusResponse;
import com.app.APP.model.enums.FriendshipStatus;
import com.app.APP.repository.FriendshipRepository;
import com.app.APP.repository.FollowerRepository;
import com.app.APP.repository.UserRepository;
import com.app.APP.stub.FriendshipStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@DisplayName("FollowerService")
class FollowerServiceTest {

    private static final String MY_ID = "usuario-1";
    private static final String OTHER_ID = "usuario-2";
    private static final String OTHER_CODE = "outro#2";

    @Mock
    private FollowerRepository followerRepository;
    @Mock
    private FriendshipRepository friendshipRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FollowerService service;

    /** Target resolved by userCode (User entity is @Immutable). */
    private User target() {
        User user = mock(User.class);
        when(user.getId()).thenReturn(OTHER_ID);
        return user;
    }

    // ---- follow ----

    @Test
    @DisplayName("follow should persist directional relation without acceptance")
    void shouldFollow() {
        final User other = target();
        when(userRepository.findByUserCode(OTHER_CODE)).thenReturn(Optional.of(other));
        when(friendshipRepository.findRelation(MY_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(followerRepository.existsByFollowerIdAndFollowedId(MY_ID, OTHER_ID)).thenReturn(false);

        service.follow(MY_ID, OTHER_CODE);

        ArgumentCaptor<Follower> captor = ArgumentCaptor.forClass(Follower.class);
        verify(followerRepository).save(captor.capture());
        assertThat(captor.getValue().getFollowerId()).isEqualTo(MY_ID);
        assertThat(captor.getValue().getFollowedId()).isEqualTo(OTHER_ID);
        assertThat(captor.getValue().getId()).isNotNull();
    }

    @Test
    @DisplayName("follow should be idempotent when already following")
    void shouldIgnoreDuplicateFollow() {
        final User other = target();
        when(userRepository.findByUserCode(OTHER_CODE)).thenReturn(Optional.of(other));
        when(friendshipRepository.findRelation(MY_ID, OTHER_ID)).thenReturn(Optional.empty());
        when(followerRepository.existsByFollowerIdAndFollowedId(MY_ID, OTHER_ID)).thenReturn(true);

        service.follow(MY_ID, OTHER_CODE);

        verify(followerRepository, never()).save(any());
    }

    @Test
    @DisplayName("follow should fail when following yourself")
    void shouldFailSelfFollow() {
        User me = mock(User.class);
        when(me.getId()).thenReturn(MY_ID);
        when(userRepository.findByUserCode("eu#1")).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> service.follow(MY_ID, "eu#1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("seguir a si mesmo");

        verify(followerRepository, never()).save(any());
    }

    @Test
    @DisplayName("follow should fail when there is a block between users")
    void shouldFailFollowBlocked() {
        final User other = target();
        when(userRepository.findByUserCode(OTHER_CODE)).thenReturn(Optional.of(other));
        when(friendshipRepository.findRelation(MY_ID, OTHER_ID))
                .thenReturn(Optional.of(FriendshipStub.aFriendship().status(FriendshipStatus.BLOQUEADA).build()));

        assertThatThrownBy(() -> service.follow(MY_ID, OTHER_CODE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("bloqueio");

        verify(followerRepository, never()).save(any());
    }

    @Test
    @DisplayName("follow should fail when target does not exist")
    void shouldFailFollowUnknownTarget() {
        when(userRepository.findByUserCode("fantasma#9")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.follow(MY_ID, "fantasma#9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrado");
    }

    // ---- unfollow ----

    @Test
    @DisplayName("unfollow should remove the relation (friendship untouched)")
    void shouldUnfollow() {
        final User other = target();
        when(userRepository.findByUserCode(OTHER_CODE)).thenReturn(Optional.of(other));

        service.unfollow(MY_ID, OTHER_CODE);

        verify(followerRepository).deleteByFollowerIdAndFollowedId(MY_ID, OTHER_ID);
    }

    // ---- status / counters ----

    @Test
    @DisplayName("status should report following, followsMe and mutual")
    void shouldReportStatus() {
        final User other = target();
        when(userRepository.findByUserCode(OTHER_CODE)).thenReturn(Optional.of(other));
        when(followerRepository.existsByFollowerIdAndFollowedId(MY_ID, OTHER_ID)).thenReturn(true);
        when(followerRepository.existsByFollowerIdAndFollowedId(OTHER_ID, MY_ID)).thenReturn(true);

        FollowStatusResponse status = service.status(MY_ID, OTHER_CODE);

        assertThat(status.following()).isTrue();
        assertThat(status.followsMe()).isTrue();
        assertThat(status.mutual()).isTrue();
    }

    @Test
    @DisplayName("counters should return followers and following totals")
    void shouldCount() {
        final User other = target();
        when(userRepository.findByUserCode(OTHER_CODE)).thenReturn(Optional.of(other));
        when(followerRepository.countByFollowedId(OTHER_ID)).thenReturn(7L);
        when(followerRepository.countByFollowerId(OTHER_ID)).thenReturn(3L);

        CountersResponse counters = service.counters(OTHER_CODE);

        assertThat(counters.followers()).isEqualTo(7);
        assertThat(counters.following()).isEqualTo(3);
    }

    // ---- integracao com o loc ----

    @Test
    @DisplayName("isFollower should check by ids for the location service")
    void shouldCheckFollowerByIds() {
        when(followerRepository.existsByFollowerIdAndFollowedId(MY_ID, OTHER_ID)).thenReturn(true);

        assertThat(service.isFollower(MY_ID, OTHER_ID)).isTrue();
    }

    @Test
    @DisplayName("followingIds should return followed ids for batch checks")
    void shouldListFollowingIds() {
        when(followerRepository.findFollowedIds(MY_ID)).thenReturn(List.of(OTHER_ID, "usuario-3"));

        assertThat(service.followingIds(MY_ID)).containsExactly(OTHER_ID, "usuario-3");
    }
}
