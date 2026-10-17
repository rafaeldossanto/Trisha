package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Path;
import com.app.APP.model.dto.request.PathRequest;
import com.app.APP.model.dto.response.PathDiscoveryResponse;
import com.app.APP.model.dto.response.PathResponse;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.PathRepository;
import com.app.APP.stub.AdventureStub;
import com.app.APP.stub.PathStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PathService")
class PathServiceTest {

    @Mock
    private PathRepository pathRepository;
    @Mock
    private AdventureRepository adventureRepository;
    @Mock
    private AdventureAccessService accessService;

    @InjectMocks
    private PathService service;

    @Test
    @DisplayName("start should persist path when adventure exists")
    void shouldStartPath() {
        PathRequest request = PathStub.aRequest();
        Adventure adventure = AdventureStub.anAdventure().build();
        Path saved = PathStub.aPath().build();

        when(adventureRepository.findByIdForUpdate(request.adventureId())).thenReturn(Optional.of(adventure));
        when(pathRepository.countByAdventureId(request.adventureId())).thenReturn(2);
        when(pathRepository.save(any(Path.class))).thenReturn(saved);

        PathResponse response = service.start(PathStub.USER_ID, request);

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.userId()).isEqualTo(PathStub.USER_ID);

        ArgumentCaptor<Path> captor = ArgumentCaptor.forClass(Path.class);
        verify(pathRepository).save(captor.capture());
        assertThat(captor.getValue().getNumber()).isEqualTo(3); // count(2) + 1
    }

    @Test
    @DisplayName("start should fail when adventure does not exist")
    void shouldFailStartWithoutAdventure() {
        PathRequest request = PathStub.aRequest();
        when(adventureRepository.findByIdForUpdate(request.adventureId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(PathStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aventura nao encontrada");

        verify(pathRepository, never()).save(any());
    }

    @Test
    @DisplayName("start should fail when user is not owner nor participant of the adventure")
    void shouldFailStartWithoutBond() {
        PathRequest request = PathStub.aRequest();
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findByIdForUpdate(request.adventureId())).thenReturn(Optional.of(adventure));
        doThrow(new IllegalArgumentException("Voce nao participa desta aventura"))
                .when(accessService).validateContribute("intruso", adventure);

        assertThatThrownBy(() -> service.start("intruso", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao participa");

        verify(pathRepository, never()).save(any());
    }

    @Test
    @DisplayName("finish should record end date and distance")
    void shouldFinishPath() {
        Path path = PathStub.aPath().build();
        when(pathRepository.findById(PathStub.ID)).thenReturn(Optional.of(path));
        when(pathRepository.save(any(Path.class))).thenAnswer(inv -> inv.getArgument(0));

        PathResponse response = service.finish(PathStub.USER_ID, PathStub.ID, 12.5);

        assertThat(response.totalDistanceKm()).isEqualTo(12.5);
        assertThat(response.finishedAt()).isNotNull();
    }

    @Test
    @DisplayName("finish should fail when caller does not own the path")
    void shouldFailFinishNotOwner() {
        Path path = PathStub.aPath().build();
        when(pathRepository.findById(PathStub.ID)).thenReturn(Optional.of(path));

        assertThatThrownBy(() -> service.finish("intruso", PathStub.ID, 12.5))
                .isInstanceOf(com.app.APP.exception.ForbiddenException.class)
                .hasMessageContaining("nao e o dono deste caminho");

        verify(pathRepository, never()).save(any());
    }

    @Test
    @DisplayName("finish should fail when path does not exist")
    void shouldFailFinishNotFound() {
        when(pathRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.finish(PathStub.USER_ID, "inexistente", 1.0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Caminho nao encontrado");
    }

    @Test
    @DisplayName("getByAdventure should map page")
    void shouldListByAdventure() {
        Pageable pageable = PageRequest.of(0, 10);
        when(adventureRepository.findById(AdventureStub.ID))
                .thenReturn(Optional.of(AdventureStub.anAdventure().build()));
        when(pathRepository.findByAdventureId(AdventureStub.ID, pageable))
                .thenReturn(new PageImpl<>(List.of(PathStub.aPath().build())));

        Page<PathResponse> response = service.getByAdventure(PathStub.USER_ID, AdventureStub.ID, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).adventureId()).isEqualTo(AdventureStub.ID);
    }

    @Test
    @DisplayName("getByUser should map page")
    void shouldListByUser() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pathRepository.findByUserId(PathStub.USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(PathStub.aPath().build())));

        Page<PathResponse> response = service.getByUser(PathStub.USER_ID, PathStub.USER_ID, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("discover should map paths visible to the observer")
    void shouldDiscoverVisiblePaths() {
        List<String> ids = List.of(PathStub.ID, "caminho-privado");
        when(pathRepository.findDiscoverableByIds(ids, "observador-1"))
                .thenReturn(List.of(PathStub.aPath().build()));

        List<PathDiscoveryResponse> response = service.discover("observador-1", ids);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).id()).isEqualTo(PathStub.ID);
        assertThat(response.get(0).adventureId()).isEqualTo(AdventureStub.ID);
        assertThat(response.get(0).destination()).isEqualTo(AdventureStub.DESTINATION);
    }

    @Test
    @DisplayName("discover should return empty without querying when there are no ids")
    void shouldDiscoverNothingWithoutIds() {
        List<PathDiscoveryResponse> response = service.discover("observador-1", List.of());

        assertThat(response).isEmpty();
        verify(pathRepository, never()).findDiscoverableByIds(any(), any());
    }
}
