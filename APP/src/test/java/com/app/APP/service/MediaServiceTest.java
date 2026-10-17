package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Media;
import com.app.APP.entity.Path;
import com.app.APP.model.dto.request.MediaRequest;
import com.app.APP.model.dto.response.MediaResponse;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.PathRepository;
import com.app.APP.repository.MediaRepository;
import com.app.APP.stub.AdventureStub;
import com.app.APP.stub.PathStub;
import com.app.APP.stub.MediaStub;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MediaService")
class MediaServiceTest {

    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private AdventureRepository adventureRepository;
    @Mock
    private PathRepository pathRepository;
    @Mock
    private AdventureAccessService accessService;

    @InjectMocks
    private MediaService service;

    @Test
    @DisplayName("save should persist media with associated path")
    void shouldSaveWithPath() {
        MediaRequest request = MediaStub.aRequest();
        Adventure adventure = AdventureStub.anAdventure().build();
        Path path = PathStub.aPath().build();
        when(adventureRepository.findById(request.adventureId())).thenReturn(Optional.of(adventure));
        when(pathRepository.findById(request.pathId())).thenReturn(Optional.of(path));
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaResponse response = service.save(MediaStub.USER_ID, request);

        assertThat(response.url()).isEqualTo(request.url());
        assertThat(response.pathId()).isEqualTo(PathStub.ID);
        verify(mediaRepository).save(any(Media.class));
    }

    @Test
    @DisplayName("save should persist standalone media (without path)")
    void shouldSaveWithoutPath() {
        MediaRequest request = MediaStub.aRequestWithoutPath();
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(request.adventureId())).thenReturn(Optional.of(adventure));
        when(mediaRepository.save(any(Media.class))).thenAnswer(inv -> inv.getArgument(0));

        MediaResponse response = service.save(MediaStub.USER_ID, request);

        assertThat(response.pathId()).isNull();
        verify(pathRepository, never()).findById(any());
    }

    @Test
    @DisplayName("save should fail when adventure does not exist")
    void shouldFailWithoutAdventure() {
        MediaRequest request = MediaStub.aRequest();
        when(adventureRepository.findById(request.adventureId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(MediaStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aventura nao encontrada");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    @DisplayName("save should fail when provided path does not exist")
    void shouldFailWithPathNotFound() {
        MediaRequest request = MediaStub.aRequest();
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(request.adventureId())).thenReturn(Optional.of(adventure));
        when(pathRepository.findById(request.pathId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.save(MediaStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Caminho nao encontrado");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    @DisplayName("save should fail when user is not owner nor participant of the adventure")
    void shouldFailSaveWithoutBond() {
        MediaRequest request = MediaStub.aRequest();
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(request.adventureId())).thenReturn(Optional.of(adventure));
        doThrow(new IllegalArgumentException("Voce nao participa desta aventura"))
                .when(accessService).validateContribute("intruso", adventure);

        assertThatThrownBy(() -> service.save("intruso", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao participa");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    @DisplayName("save should fail when the path belongs to another adventure")
    void shouldFailSavePathFromAnotherAdventure() {
        MediaRequest request = MediaStub.aRequest();
        Adventure adventure = AdventureStub.anAdventure().build();
        Adventure other = AdventureStub.anAdventure().id("aventura-2").build();
        Path pathFromOther = PathStub.aPath().adventure(other).build();
        when(adventureRepository.findById(request.adventureId())).thenReturn(Optional.of(adventure));
        when(pathRepository.findById(request.pathId())).thenReturn(Optional.of(pathFromOther));

        assertThatThrownBy(() -> service.save(MediaStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao pertence a essa aventura");

        verify(mediaRepository, never()).save(any());
    }

    @Test
    @DisplayName("getByAdventure should map page")
    void shouldListByAdventure() {
        Pageable pageable = PageRequest.of(0, 10);
        when(adventureRepository.findById(AdventureStub.ID))
                .thenReturn(Optional.of(AdventureStub.anAdventure().build()));
        when(mediaRepository.findByAdventureId(AdventureStub.ID, pageable))
                .thenReturn(new PageImpl<>(List.of(MediaStub.aMedia().build())));

        Page<MediaResponse> response = service.getByAdventure(AdventureStub.USER_ID, AdventureStub.ID, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).adventureId()).isEqualTo(AdventureStub.ID);
    }

    @Test
    @DisplayName("getByPath should map page")
    void shouldListByPath() {
        Pageable pageable = PageRequest.of(0, 10);
        when(pathRepository.findById(PathStub.ID))
                .thenReturn(Optional.of(PathStub.aPath().build()));
        when(mediaRepository.findByPathId(PathStub.ID, pageable))
                .thenReturn(new PageImpl<>(List.of(MediaStub.aMedia().build())));

        Page<MediaResponse> response = service.getByPath(PathStub.USER_ID, PathStub.ID, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("delete should remove existing media")
    void shouldDelete() {
        Media media = MediaStub.aMedia().build();
        when(mediaRepository.findById(MediaStub.ID)).thenReturn(Optional.of(media));

        service.delete(MediaStub.USER_ID, MediaStub.ID);

        verify(mediaRepository).delete(media);
    }

    @Test
    @DisplayName("delete should fail when media does not exist")
    void shouldFailDeleteNotFound() {
        when(mediaRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(MediaStub.USER_ID, "inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Midia nao encontrada");
    }
}
