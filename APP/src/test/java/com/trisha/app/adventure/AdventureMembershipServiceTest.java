package com.trisha.app.adventure;

import com.trisha.app.adventure.dto.LeaveAdventureResponse;
import com.trisha.app.media.Media;
import com.trisha.app.media.MediaRepository;
import com.trisha.app.media.MediaService;
import com.trisha.app.path.Path;
import com.trisha.app.path.PathRepository;
import com.trisha.app.path.PathService;
import com.trisha.app.point.PointOfInterestService;
import com.trisha.app.shared.exception.ForbiddenException;
import com.trisha.app.stub.AdventureStub;
import com.trisha.app.stub.PathStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdventureMembershipService")
class AdventureMembershipServiceTest {

    private static final String OWNER = AdventureStub.USER_ID;
    private static final String MEMBER = "membro-2";
    private static final String ADV_ID = AdventureStub.ID;

    @Mock private AdventureRepository adventureRepository;
    @Mock private AdventureParticipantRepository participantRepository;
    @Mock private PathRepository pathRepository;
    @Mock private MediaRepository mediaRepository;
    @Mock private AdventureService adventureService;
    @Mock private PathService pathService;
    @Mock private MediaService mediaService;
    @Mock private PointOfInterestService pointService;

    @InjectMocks
    private AdventureMembershipService service;

    private Media media(String userId, Adventure adventure) {
        return Media.builder().id("midia-" + userId).adventure(adventure).userId(userId).build();
    }

    private AdventureParticipant participant(String userId, Adventure adventure) {
        return AdventureParticipant.builder().id("part-" + userId).adventure(adventure).userId(userId).build();
    }

    private Adventure personalOf(String userId) {
        return AdventureStub.anAdventure().id("pessoal-" + userId).userId(userId)
                .region(null).visibility(AdventureVisibility.PRIVADA).build();
    }

    // ---- addParticipant ----

    @Test
    @DisplayName("addParticipant should persist when owner invites a new participant")
    void shouldAddParticipant() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, "usuario-9")).thenReturn(false);

        service.addParticipant(OWNER, ADV_ID, "usuario-9");

        ArgumentCaptor<AdventureParticipant> captor = ArgumentCaptor.forClass(AdventureParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("usuario-9");
    }

    @Test
    @DisplayName("addParticipant should fail when caller does not own the adventure")
    void shouldFailAddParticipantNotOwner() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));

        // Sem o gate de dono, qualquer um se adicionaria a uma aventura SO_GRUPO
        // e ganharia acesso de leitura a ela.
        assertThatThrownBy(() -> service.addParticipant("intruso", ADV_ID, "intruso"))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("nao e o dono");

        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("addParticipant should fail when user is already participating")
    void shouldFailDuplicateParticipant() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, "usuario-9")).thenReturn(true);

        assertThatThrownBy(() -> service.addParticipant(OWNER, ADV_ID, "usuario-9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja participa");

        verify(participantRepository, never()).save(any());
    }

    // ---- leave ----

    @Test
    @DisplayName("leave keeping data moves paths/media to a new personal adventure")
    void leaveKeepingDataMoves() {
        Adventure adventure = AdventureStub.anAdventure().build();
        Adventure personal = personalOf(MEMBER);
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER))
                .thenReturn(List.of(PathStub.aPath().userId(MEMBER).build()));
        when(adventureService.createPersonalCopy(adventure, MEMBER)).thenReturn(personal);
        when(pathService.reassign(ADV_ID, MEMBER, personal)).thenReturn(1);

        LeaveAdventureResponse response = service.leave(MEMBER, ADV_ID, true);

        assertThat(response.personalAdventureId()).isEqualTo(personal.getId());
        assertThat(response.movedPaths()).isEqualTo(1);
        assertThat(response.deletedPaths()).isZero();
        verify(mediaService).reassign(ADV_ID, MEMBER, personal);
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
        verifyNoInteractions(pointService);
    }

    @Test
    @DisplayName("leave keeping data with only media still moves it to a personal adventure")
    void leaveKeepingOnlyMediaMoves() {
        Adventure adventure = AdventureStub.anAdventure().build();
        Adventure personal = personalOf(MEMBER);
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of());
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(media(MEMBER, adventure)));
        when(adventureService.createPersonalCopy(adventure, MEMBER)).thenReturn(personal);

        LeaveAdventureResponse response = service.leave(MEMBER, ADV_ID, true);

        assertThat(response.personalAdventureId()).isEqualTo(personal.getId());
        assertThat(response.movedPaths()).isZero();
        verify(mediaService).reassign(ADV_ID, MEMBER, personal);
    }

    @Test
    @DisplayName("leave keeping data without paths or media creates no personal adventure")
    void leaveKeepingDataWithoutDataCreatesNothing() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of());
        when(mediaRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of());

        LeaveAdventureResponse response = service.leave(MEMBER, ADV_ID, true);

        assertThat(response.personalAdventureId()).isNull();
        assertThat(response.movedPaths()).isZero();
        verifyNoInteractions(adventureService, pathService, mediaService, pointService);
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
    }

    @Test
    @DisplayName("leave discarding data deletes points, then media, then paths, and creates no adventure")
    void leaveDiscardingDataCascades() {
        Adventure adventure = AdventureStub.anAdventure().build();
        Path path = PathStub.aPath().userId(MEMBER).build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(path));
        when(pathService.deleteByAdventureAndUser(ADV_ID, MEMBER)).thenReturn(1);

        LeaveAdventureResponse response = service.leave(MEMBER, ADV_ID, false);

        assertThat(response.personalAdventureId()).isNull();
        assertThat(response.movedPaths()).isZero();
        assertThat(response.deletedPaths()).isEqualTo(1);
        InOrder order = inOrder(pointService, mediaService, pathService, participantRepository);
        order.verify(pointService).deleteByPathIds(List.of(PathStub.ID));
        order.verify(mediaService).deleteByAdventureAndUser(ADV_ID, MEMBER);
        order.verify(pathService).deleteByAdventureAndUser(ADV_ID, MEMBER);
        order.verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
        verify(adventureService, never()).createPersonalCopy(any(), any());
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
        Adventure personal = personalOf(MEMBER);
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(true);
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER))
                .thenReturn(List.of(PathStub.aPath().userId(MEMBER).build()));
        when(adventureService.createPersonalCopy(adventure, MEMBER)).thenReturn(personal);
        when(pathService.reassign(ADV_ID, MEMBER, personal)).thenReturn(1);

        LeaveAdventureResponse response = service.kick(OWNER, ADV_ID, MEMBER);

        assertThat(response.personalAdventureId()).isEqualTo(personal.getId());
        assertThat(response.movedPaths()).isEqualTo(1);
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
        verify(pathService, never()).deleteByAdventureAndUser(any(), any());
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

    // ---- delete ----

    @Test
    @DisplayName("delete preserves each member's data and discards the owner's, then removes the adventure")
    void deletePreservesMembersDiscardsOwner() {
        Adventure adventure = AdventureStub.anAdventure().build();
        Adventure personal = personalOf(MEMBER);
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.findByAdventureId(ADV_ID))
                .thenReturn(List.of(participant(OWNER, adventure), participant(MEMBER, adventure)));

        Path memberPath = PathStub.aPath().id("cam-membro").userId(MEMBER).build();
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, MEMBER)).thenReturn(List.of(memberPath));
        when(adventureService.createPersonalCopy(adventure, MEMBER)).thenReturn(personal);

        Path ownerPath = PathStub.aPath().id("cam-dono").userId(OWNER).build();
        when(pathRepository.findByAdventureIdAndUserId(ADV_ID, OWNER)).thenReturn(List.of(ownerPath));

        service.delete(OWNER, ADV_ID);

        verify(pathService).reassign(ADV_ID, MEMBER, personal);
        verify(mediaService).reassign(ADV_ID, MEMBER, personal);
        verify(participantRepository).deleteByAdventureIdAndUserId(ADV_ID, MEMBER);
        verify(pathService, never()).deleteByAdventureAndUser(ADV_ID, MEMBER);

        InOrder order = inOrder(pointService, mediaService, pathService, participantRepository, adventureRepository);
        order.verify(pointService).deleteByPathIds(List.of("cam-dono"));
        order.verify(mediaService).deleteByAdventureAndUser(ADV_ID, OWNER);
        order.verify(pathService).deleteByAdventureAndUser(ADV_ID, OWNER);
        order.verify(participantRepository).deleteByAdventureId(ADV_ID);
        order.verify(adventureRepository).delete(adventure);
    }

    @Test
    @DisplayName("delete by a non-owner is forbidden and removes nothing")
    void nonOwnerCannotDelete() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(ADV_ID)).thenReturn(Optional.of(adventure));

        assertThatThrownBy(() -> service.delete("intruso", ADV_ID))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("nao e o dono");

        verifyNoInteractions(pathService, mediaService, pointService);
        verify(adventureRepository, never()).delete(any());
    }
}
