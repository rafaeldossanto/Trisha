package com.trisha.app.point;

import com.trisha.app.adventure.AdventureAccessService;
import com.trisha.app.path.Path;
import com.trisha.app.path.PathRepository;
import com.trisha.app.point.dto.EvidenceRequest;
import com.trisha.app.point.dto.EvidenceResponse;
import com.trisha.app.point.dto.PointOfInterestRequest;
import com.trisha.app.point.dto.PointOfInterestResponse;
import com.trisha.app.stub.PathStub;
import com.trisha.app.stub.PointOfInterestStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PointOfInterestService")
class PointOfInterestServiceTest {

    @Mock
    private PointOfInterestRepository pointRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private PointOfInterestUserStatusRepository statusRepository;
    @Mock
    private PathRepository pathRepository;
    @Mock
    private AdventureAccessService accessService;

    @InjectMocks
    private PointOfInterestService service;

    @Test
    @DisplayName("create should persist point when path exists")
    void shouldCreatePoint() {
        PointOfInterestRequest request = PointOfInterestStub.aRequest();
        Path path = PathStub.aPath().build();
        when(pathRepository.findById(request.pathId())).thenReturn(Optional.of(path));
        when(pointRepository.save(any(PointOfInterest.class))).thenAnswer(inv -> inv.getArgument(0));

        PointOfInterestResponse response = service.create(PointOfInterestStub.USER_ID, request);

        assertThat(response.name()).isEqualTo(request.name());
        assertThat(response.type()).isEqualTo(request.type());
        // Recem-criado com descricao e sem evidencias: nivel 2 (nao o 1 fixo de antes).
        assertThat(response.confidenceLevel()).isEqualTo(2);
        verify(pointRepository).save(any(PointOfInterest.class));
    }

    @Test
    @DisplayName("create should fail when user is not owner nor participant of the adventure")
    void shouldFailCreateWithoutBond() {
        PointOfInterestRequest request = PointOfInterestStub.aRequest();
        Path path = PathStub.aPath().build();
        when(pathRepository.findById(request.pathId())).thenReturn(Optional.of(path));
        doThrow(new IllegalArgumentException("Voce nao participa desta aventura"))
                .when(accessService).validateContribute("intruso", path.getAdventure());

        assertThatThrownBy(() -> service.create("intruso", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao participa");

        verify(pointRepository, never()).save(any());
    }

    @Test
    @DisplayName("create should fail when path does not exist")
    void shouldFailCreateWithoutPath() {
        PointOfInterestRequest request = PointOfInterestStub.aRequest();
        when(pathRepository.findById(request.pathId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(PointOfInterestStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Caminho nao encontrado");

        verify(pointRepository, never()).save(any());
    }

    @Test
    @DisplayName("addEvidence should accept capture within 50m radius")
    void shouldAcceptCloseEvidence() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        EvidenceRequest request = PointOfInterestStub.aCloseEvidenceRequest();
        when(pointRepository.findById(request.pointId())).thenReturn(Optional.of(point));
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(inv -> inv.getArgument(0));

        EvidenceResponse response = service.addEvidence(PointOfInterestStub.USER_ID, request);

        assertThat(response.validated()).isTrue();
        assertThat(response.pointId()).isEqualTo(point.getId());
        verify(evidenceRepository).save(any(Evidence.class));
    }

    @Test
    @DisplayName("addEvidence should reject capture beyond 50m")
    void shouldRejectFarEvidence() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        EvidenceRequest request = PointOfInterestStub.aFarEvidenceRequest();
        when(pointRepository.findById(request.pointId())).thenReturn(Optional.of(point));

        assertThatThrownBy(() -> service.addEvidence(PointOfInterestStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("muito longe");

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("addEvidence should fail when point does not exist")
    void shouldFailEvidenceWithoutPoint() {
        EvidenceRequest request = PointOfInterestStub.aCloseEvidenceRequest();
        when(pointRepository.findById(request.pointId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.addEvidence(PointOfInterestStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Ponto nao encontrado");
    }

    @Test
    @DisplayName("addEvidence should fail when user cannot view the point's adventure")
    void shouldFailEvidenceWithoutAccess() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        EvidenceRequest request = PointOfInterestStub.aCloseEvidenceRequest();
        when(pointRepository.findById(request.pointId())).thenReturn(Optional.of(point));
        doThrow(new IllegalArgumentException("Aventura nao encontrada ou sem acesso"))
                .when(accessService).validateView("intruso", point.getPath().getAdventure());

        assertThatThrownBy(() -> service.addEvidence("intruso", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");

        verify(evidenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById should calculate maximum level (3+ users and description)")
    void shouldCalculateMaximumLevel() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        when(pointRepository.findById(point.getId())).thenReturn(Optional.of(point));
        when(evidenceRepository.countValidatedUsersByPointId(point.getId())).thenReturn(3L);

        PointOfInterestResponse response = service.getById(PointOfInterestStub.USER_ID, point.getId());

        assertThat(response.confidenceLevel()).isEqualTo(5);
    }

    @Test
    @DisplayName("getById should calculate minimum level (no evidence, no description)")
    void shouldCalculateMinimumLevel() {
        PointOfInterest point = PointOfInterestStub.aPoint().description(null).build();
        when(pointRepository.findById(point.getId())).thenReturn(Optional.of(point));
        when(evidenceRepository.countValidatedUsersByPointId(point.getId())).thenReturn(0L);

        PointOfInterestResponse response = service.getById(PointOfInterestStub.USER_ID, point.getId());

        assertThat(response.confidenceLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("getById should fail when observer cannot view the point's adventure")
    void shouldFailGetByIdWithoutAccess() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        when(pointRepository.findById(point.getId())).thenReturn(Optional.of(point));
        doThrow(new IllegalArgumentException("Aventura nao encontrada ou sem acesso"))
                .when(accessService).validateView("intruso", point.getPath().getAdventure());

        assertThatThrownBy(() -> service.getById("intruso", point.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");
    }

    @Test
    @DisplayName("getByPath should paginate, map and calculate level via batch count")
    void shouldListByPath() {
        Pageable pageable = PageRequest.of(0, 10);
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        when(accessService.canViewPath(PointOfInterestStub.USER_ID, PathStub.ID)).thenReturn(true);
        when(pointRepository.findByPathId(PathStub.ID, pageable))
                .thenReturn(new PageImpl<>(List.of(point)));
        // batch count: [pointId, total] — 1 validated user for the point
        when(evidenceRepository.countValidatedUsersPerPoint(List.of(point.getId())))
                .thenReturn(List.<Object[]>of(new Object[]{point.getId(), 1L}));

        Page<PointOfInterestResponse> response =
                service.getByPath(PointOfInterestStub.USER_ID, PathStub.ID, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).confidenceLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("getByPath should fail without exposing points when observer lacks access")
    void shouldFailListByPathWithoutAccess() {
        when(accessService.canViewPath("intruso", PathStub.ID)).thenReturn(false);

        assertThatThrownBy(() -> service.getByPath("intruso", PathStub.ID, PageRequest.of(0, 10)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");

        verify(pointRepository, never()).findByPathId(any(), any());
    }

    // ---- deleteByPathIds (descarte de dados ao sair/excluir aventura) ----

    @Test
    @DisplayName("deleteByPathIds should delete status, evidence and points of the paths, in this order")
    void shouldDeletePointsByPathIds() {
        PointOfInterest point = PointOfInterestStub.aPoint().build();
        when(pointRepository.findByPathIdIn(List.of(PathStub.ID))).thenReturn(List.of(point));

        service.deleteByPathIds(List.of(PathStub.ID));

        InOrder order = inOrder(statusRepository, evidenceRepository, pointRepository);
        order.verify(statusRepository).deleteByPointIdIn(List.of(point.getId()));
        order.verify(evidenceRepository).deleteByPointIdIn(List.of(point.getId()));
        order.verify(pointRepository).deleteAll(List.of(point));
    }

    @Test
    @DisplayName("deleteByPathIds should only delete the (empty) point list when the paths have no points")
    void shouldSkipStatusAndEvidenceWithoutPoints() {
        when(pointRepository.findByPathIdIn(List.of(PathStub.ID))).thenReturn(List.of());

        service.deleteByPathIds(List.of(PathStub.ID));

        verify(statusRepository, never()).deleteByPointIdIn(any());
        verify(evidenceRepository, never()).deleteByPointIdIn(any());
        verify(pointRepository).deleteAll(List.of());
    }

    @Test
    @DisplayName("deleteByPathIds should not query anything when there are no paths")
    void shouldDeleteNothingWithoutPaths() {
        service.deleteByPathIds(List.of());

        verify(pointRepository, never()).findByPathIdIn(any());
        verify(pointRepository, never()).deleteAll(any());
    }
}
