package com.app.APP.service;

import com.app.APP.entity.PointOfInterest;
import com.app.APP.entity.PointOfInterestUserStatus;
import com.app.APP.model.dto.response.PointStatusResponse;
import com.app.APP.model.enums.PointStatus;
import com.app.APP.repository.PointOfInterestRepository;
import com.app.APP.repository.PointOfInterestUserStatusRepository;
import com.app.APP.stub.PointOfInterestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointOfInterestStatusService")
class PointOfInterestStatusServiceTest {

    private static final String USER_ID = PointOfInterestStub.USER_ID;
    private static final String POINT_ID = PointOfInterestStub.ID;

    @Mock
    private PointOfInterestUserStatusRepository statusRepository;
    @Mock
    private PointOfInterestRepository pointRepository;
    @Mock
    private AdventureAccessService accessService;

    @InjectMocks
    private PointOfInterestStatusService service;

    private PointOfInterestUserStatus.PointOfInterestUserStatusBuilder aMark() {
        return PointOfInterestUserStatus.builder()
                .id("marcacao-1")
                .userId(USER_ID)
                .pointId(POINT_ID)
                .createdAt(LocalDateTime.now());
    }

    private void givenAccessiblePoint() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        when(pointRepository.findById(POINT_ID)).thenReturn(Optional.of(point));
    }

    @Test
    @DisplayName("setStatus should create the mark when none exists")
    void shouldCreateMarkOnFirstStatus() {
        givenAccessiblePoint();
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.empty());
        when(statusRepository.save(any(PointOfInterestUserStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PointStatusResponse response = service.setStatus(USER_ID, POINT_ID, PointStatus.NO_RADAR);

        assertThat(response.pointId()).isEqualTo(POINT_ID);
        assertThat(response.status()).isEqualTo(PointStatus.NO_RADAR);
        assertThat(response.goal()).isFalse();
        verify(statusRepository).save(any(PointOfInterestUserStatus.class));
    }

    @Test
    @DisplayName("setStatus should update the existing mark keeping the goal flag")
    void shouldUpdateExistingMark() {
        givenAccessiblePoint();
        PointOfInterestUserStatus existing = aMark().status(PointStatus.NO_RADAR).goal(true).build();
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.of(existing));
        when(statusRepository.save(any(PointOfInterestUserStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PointStatusResponse response = service.setStatus(USER_ID, POINT_ID, PointStatus.CONQUISTADO);

        assertThat(response.status()).isEqualTo(PointStatus.CONQUISTADO);
        assertThat(response.goal()).isTrue();
        assertThat(existing.getId()).isEqualTo("marcacao-1");
    }

    @Test
    @DisplayName("setStatus should fail when point does not exist")
    void shouldFailStatusWithoutPoint() {
        when(pointRepository.findById(POINT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setStatus(USER_ID, POINT_ID, PointStatus.NA_MIRA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ponto nao encontrado");

        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("setStatus should fail when user cannot view the point")
    void shouldFailStatusWithoutAccess() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        when(pointRepository.findById(POINT_ID)).thenReturn(Optional.of(point));
        doThrow(new IllegalArgumentException("Sem acesso a esta aventura"))
                .when(accessService).validateView("intruso", point.getPath().getAdventure());

        assertThatThrownBy(() -> service.setStatus("intruso", POINT_ID, PointStatus.NA_MIRA))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Sem acesso");

        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("clearStatus should delete the row when goal is off")
    void shouldDeleteRowWhenClearingLastMark() {
        PointOfInterestUserStatus existing = aMark().status(PointStatus.NA_MIRA).goal(false).build();
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.of(existing));

        service.clearStatus(USER_ID, POINT_ID);

        verify(statusRepository).delete(existing);
        verify(statusRepository, never()).save(any());
    }

    @Test
    @DisplayName("clearStatus should keep the row when goal is still on")
    void shouldKeepRowWhenGoalRemains() {
        PointOfInterestUserStatus existing = aMark().status(PointStatus.NA_MIRA).goal(true).build();
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.of(existing));
        when(statusRepository.save(any(PointOfInterestUserStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        service.clearStatus(USER_ID, POINT_ID);

        assertThat(existing.getStatus()).isNull();
        assertThat(existing.isGoal()).isTrue();
        verify(statusRepository, never()).delete(any());
    }

    @Test
    @DisplayName("setGoal true should create the mark validating access")
    void shouldCreateMarkOnGoal() {
        givenAccessiblePoint();
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.empty());
        when(statusRepository.save(any(PointOfInterestUserStatus.class))).thenAnswer(inv -> inv.getArgument(0));

        PointStatusResponse response = service.setGoal(USER_ID, POINT_ID, true);

        assertThat(response.goal()).isTrue();
        assertThat(response.status()).isNull();
    }

    @Test
    @DisplayName("setGoal false should delete the row when there is no status")
    void shouldDeleteRowWhenUnsettingLastGoal() {
        PointOfInterestUserStatus existing = aMark().status(null).goal(true).build();
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.of(existing));

        PointStatusResponse response = service.setGoal(USER_ID, POINT_ID, false);

        assertThat(response.goal()).isFalse();
        verify(statusRepository).delete(existing);
    }

    @Test
    @DisplayName("setGoal false without mark should be a no-op")
    void shouldIgnoreUnsetGoalWithoutMark() {
        when(statusRepository.findByUserIdAndPointId(USER_ID, POINT_ID)).thenReturn(Optional.empty());

        PointStatusResponse response = service.setGoal(USER_ID, POINT_ID, false);

        assertThat(response.pointId()).isEqualTo(POINT_ID);
        assertThat(response.status()).isNull();
        assertThat(response.goal()).isFalse();
        verify(statusRepository, never()).save(any());
        verify(statusRepository, never()).delete(any());
    }

    @Test
    @DisplayName("getStatuses should return only marked points")
    void shouldListStatusesInBatch() {
        PointOfInterestUserStatus mark = aMark().status(PointStatus.CONQUISTADO).goal(true).build();
        when(statusRepository.findByUserIdAndPointIdIn(USER_ID, List.of(POINT_ID, "ponto-2")))
                .thenReturn(List.of(mark));

        List<PointStatusResponse> statuses = service.getStatuses(USER_ID, List.of(POINT_ID, "ponto-2"));

        assertThat(statuses).hasSize(1);
        assertThat(statuses.getFirst().pointId()).isEqualTo(POINT_ID);
        assertThat(statuses.getFirst().status()).isEqualTo(PointStatus.CONQUISTADO);
        assertThat(statuses.getFirst().goal()).isTrue();
    }
}
