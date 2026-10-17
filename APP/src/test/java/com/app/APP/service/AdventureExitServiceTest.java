package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.AdventureParticipant;
import com.app.APP.entity.Media;
import com.app.APP.entity.Path;
import com.app.APP.entity.PointOfInterest;
import com.app.APP.exception.ForbiddenException;
import com.app.APP.model.dto.response.LeaveAdventureResponse;
import com.app.APP.model.enums.AdventureVisibility;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.EvidenceRepository;
import com.app.APP.repository.MediaRepository;
import com.app.APP.repository.PointOfInterestRepository;
import com.app.APP.repository.PointOfInterestUserStatusRepository;
import com.app.APP.repository.PathRepository;
import com.app.APP.stub.AdventureStub;
import com.app.APP.stub.PathStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdventureExitService")
class AdventureExitServiceTest {

    private static final String OWNER = AdventureStub.USER_ID;
    private static final String MEMBER = "membro-2";
    private static final String ADV_ID = AdventureStub.ID;

    @Mock private AdventureRepository adventureRepository;
    @Mock private AdventureParticipantRepository participantRepository;
    @Mock private PathRepository pathRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private PointOfInterestRepository pointRepository;
    @Mock private EvidenceRepository evidenceRepository;
    @Mock private PointOfInterestUserStatusRepository statusRepository;

    @InjectMocks
    private AdventureExitService service;

    private Media media(String userId, Adventure adventure) {
        return Media.builder().id("midia-" + userId).adventure(adventure).userId(userId).build();
    }

    private AdventureParticipant participant(String userId, Adventure adventure) {
        return AdventureParticipant.builder().id("part-" + userId).adventure(adventure).userId(userId).build();
    }

    // ---- leave ----

    @Test
    @DisplayName("leave keeping data moves paths/media to a new PRIVATE personal adventure")
    void leaveKeepingDataMoves() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        Path path = PathStub.aPath().userId(MEMBER).build();
        Media media = media(MEMBER, adventure);
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(path));
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(media));
        when(adventureRepository.save(any(Adventure.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveAdventureResponse response = service.leave(MEMBER, ADV_ID, true);

        assertThat(response.personalAdventureId()).isNotNull();
        assertThat(response.movedPaths()).isEqualTo(1);
        assertThat(response.deletedPaths()).isZero();
        assertThat(path.getAdventure().getUserId()).isEqualTo(MEMBER);
        assertThat(path.getAdventure().getVisibility()).isEqualTo(AdventureVisibility.PRIVADA);
        assertThat(media.getAdventure().getId()).isEqualTo(response.personalAdventureId());
        verify(pathRepository).saveAll(List.of(path));
        verify(mediaRepository).saveAll(List.of(media));
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
    }

    @Test
    @DisplayName("leave discarding data cascades points/evidence/status/media/paths and creates no adventure")
    void leaveDiscardingDataCascades() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        Path path = PathStub.aPath().userId(MEMBER).build();
        Media media = media(MEMBER, adventure);
        PointOfInterest point = PointOfInterest.builder().id("p1").path(path).userId(MEMBER).build();
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(path));
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(media));
        when(pointRepository.findByPathIdIn(List.of(PathStub.ID))).thenReturn(List.of(point));

        LeaveAdventureResponse response = service.leave(MEMBER, ADV_ID, false);

        assertThat(response.personalAdventureId()).isNull();
        assertThat(response.deletedPaths()).isEqualTo(1);
        verify(statusRepository).deleteByPointIdIn(List.of("p1"));
        verify(evidenceRepository).deleteByPointIdIn(List.of("p1"));
        verify(pointRepository).deleteAll(List.of(point));
        verify(mediaRepository).deleteAll(List.of(media));
        verify(pathRepository).deleteAll(List.of(path));
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
        verify(adventureRepository, never()).save(any());
    }

    @Test
    @DisplayName("owner cannot leave their own adventure")
    void ownerCannotLeave() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> service.leave(OWNER, ADV_ID, true))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("dono nao sai");
        verify(participantRepository, never()).deleteByAdventureIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("non-participant cannot leave")
    void nonParticipantCannotLeave() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(false);

        assertThatThrownBy(() -> service.leave(MEMBER, ADV_ID, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao participa");
    }

    // ---- kick ----

    @Test
    @DisplayName("owner kicks a member and their data is always preserved")
    void ownerKicksMember() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        Path path = PathStub.aPath().userId(MEMBER).build();
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(path));
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of());
        when(adventureRepository.save(any(Adventure.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveAdventureResponse response = service.kick(OWNER, ADV_ID, MEMBER);

        assertThat(response.personalAdventureId()).isNotNull();
        assertThat(response.movedPaths()).isEqualTo(1);
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
    }

    @Test
    @DisplayName("non-owner cannot kick")
    void nonOwnerCannotKick() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> service.kick("intruso", ADV_ID, MEMBER))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("nao e o dono");
    }

    @Test
    @DisplayName("owner cannot kick themselves")
    void ownerCannotKickSelf() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> service.kick(OWNER, ADV_ID, OWNER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pode se remover");
    }

    // ---- deleteWithContent ----

    @Test
    @DisplayName("delete preserves each member's data and discards the owner's, then removes the adventure")
    void deletePreservesMembersDiscardsOwner() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(participantRepository.findByAdventureId(ADV_ID))
                .thenReturn(List.of(participant(OWNER, adventure), participant(MEMBER, adventure)));

        Path memberPath = PathStub.aPath().id("cam-membro").userId(MEMBER).build();
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(memberPath));
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of());
        when(adventureRepository.save(any(Adventure.class))).thenAnswer(inv -> inv.getArgument(0));

        Path ownerPath = PathStub.aPath().id("cam-dono").userId(OWNER).build();
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, OWNER)).thenReturn(List.of(ownerPath));
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, OWNER)).thenReturn(List.of());
        when(pointRepository.findByPathIdIn(List.of("cam-dono"))).thenReturn(List.of());

        service.deleteWithContent(adventure);

        assertThat(memberPath.getAdventure().getUserId()).isEqualTo(MEMBER);
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
        verify(pathRepository).deleteAll(List.of(ownerPath));
        verify(participantRepository).deleteByAdventureId(ADV_ID);
        verify(adventureRepository).delete(adventure);
    }
}
