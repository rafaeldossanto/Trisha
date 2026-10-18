package com.trisha.app.region;

import com.trisha.app.adventure.AdventureService;
import com.trisha.app.adventure.dto.AdventureResponse;
import com.trisha.app.region.dto.RegionRequest;
import com.trisha.app.region.dto.RegionResponse;
import com.trisha.app.social.FriendshipService;
import com.trisha.app.stub.AdventureStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RegionService")
class RegionServiceTest {

    private static final String OWNER = AdventureStub.USER_ID;
    private static final String OBSERVER = "observador-1";
    private static final String REGION_ID = AdventureStub.REGION_ID;

    @Mock
    private RegionRepository regionRepository;
    @Mock
    private FriendshipService friendshipService;
    @Mock
    private AdventureService adventureService;

    @InjectMocks
    private RegionService service;

    private final Pageable pageable = PageRequest.of(0, 10);

    private Region region(RegionVisibility visibility) {
        return AdventureStub.RegionStub.aRegion().visibility(visibility).build();
    }

    private RegionRequest aRequest() {
        return new RegionRequest("Serra do Caparao", "Regiao de montanhas",
                "http://midia/capa.jpg", RegionVisibility.PRIVADA, List.of());
    }

    // ---- create / listMine ----

    @Test
    @DisplayName("create should persist folder for the token user")
    void shouldCreate() {
        when(regionRepository.save(any(Region.class))).thenAnswer(inv -> inv.getArgument(0));

        RegionResponse response = service.create(OWNER, aRequest());

        ArgumentCaptor<Region> captor = ArgumentCaptor.forClass(Region.class);
        verify(regionRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(OWNER);
        assertThat(captor.getValue().getId()).isNotNull();
        assertThat(response.name()).isEqualTo("Serra do Caparao");
        assertThat(response.coverUrl()).isEqualTo("http://midia/capa.jpg");
    }

    @Test
    @DisplayName("listMine should page the user's folders")
    void shouldListMine() {
        when(regionRepository.findByUserId(OWNER, pageable))
                .thenReturn(new PageImpl<>(List.of(region(RegionVisibility.PRIVADA))));

        Page<RegionResponse> response = service.listMine(OWNER, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).userId()).isEqualTo(OWNER);
    }

    // ---- getById (visibilidade) ----

    @Test
    @DisplayName("getById: owner sees own PRIVADA folder")
    void ownerSeesPrivate() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.PRIVADA)));

        assertThat(service.getById(OWNER, REGION_ID).id()).isEqualTo(REGION_ID);
    }

    @Test
    @DisplayName("getById: PUBLICA folder visible to anyone")
    void publicVisibleToAnyone() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.PUBLICA)));

        assertThat(service.getById(OBSERVER, REGION_ID).id()).isEqualTo(REGION_ID);
    }

    @Test
    @DisplayName("getById: AMIGOS folder visible to accepted friends only")
    void friendsFolderForFriends() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.AMIGOS)));
        when(friendshipService.areFriends(OBSERVER, OWNER)).thenReturn(true);

        assertThat(service.getById(OBSERVER, REGION_ID).id()).isEqualTo(REGION_ID);
    }

    @Test
    @DisplayName("getById: AMIGOS folder hidden without accepted friendship")
    void friendsFolderHiddenFromStrangers() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.AMIGOS)));
        when(friendshipService.areFriends(OBSERVER, OWNER)).thenReturn(false);

        assertThatThrownBy(() -> service.getById(OBSERVER, REGION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrada ou sem acesso");
    }

    @Test
    @DisplayName("getById: someone else's PRIVADA folder is hidden")
    void privateHiddenFromOthers() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.PRIVADA)));

        assertThatThrownBy(() -> service.getById(OBSERVER, REGION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrada ou sem acesso");
    }

    // ---- update / delete (dono) ----

    @Test
    @DisplayName("update should fail when caller does not own the folder")
    void shouldFailUpdateNotOwner() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.PUBLICA)));

        assertThatThrownBy(() -> service.update(OBSERVER, REGION_ID, aRequest()))
                .isInstanceOf(com.trisha.app.shared.exception.ForbiddenException.class)
                .hasMessageContaining("nao e o dono");

        verify(regionRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should unlink adventures before removing the folder")
    void shouldDeleteUnlinkingAdventures() {
        Region region = region(RegionVisibility.PRIVADA);
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region));

        service.delete(OWNER, REGION_ID);

        InOrder order = inOrder(adventureService, regionRepository);
        order.verify(adventureService).unlinkRegion(REGION_ID);
        order.verify(regionRepository).delete(region);
    }

    // ---- discover / getAdventures ----

    @Test
    @DisplayName("discover should use sentinel when the user has no friends")
    void shouldDiscoverWithSentinel() {
        when(friendshipService.friendIds(OBSERVER)).thenReturn(List.of());
        when(regionRepository.findDiscoverable(eq(OBSERVER), eq(List.of("__sem_amigos__")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(region(RegionVisibility.PUBLICA))));

        Page<RegionResponse> response = service.discover(OBSERVER, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getAdventures should gate by folder access before listing")
    void shouldFailAdventuresWithoutAccess() {
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.PRIVADA)));

        assertThatThrownBy(() -> service.getAdventures(OBSERVER, REGION_ID, pageable))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrada ou sem acesso");

        verify(adventureService, never()).listVisibleInRegion(any(), any(), any());
    }

    @Test
    @DisplayName("getAdventures should list the adventures visible to the observer once access is granted")
    void shouldListAdventuresWithAccess() {
        Page<AdventureResponse> page = new PageImpl<>(List.of(AdventureResponse.builder().id(AdventureStub.ID).build()));
        when(regionRepository.findById(REGION_ID)).thenReturn(Optional.of(region(RegionVisibility.PUBLICA)));
        when(adventureService.listVisibleInRegion(REGION_ID, OBSERVER, pageable)).thenReturn(page);

        assertThat(service.getAdventures(OBSERVER, REGION_ID, pageable)).isSameAs(page);
    }
}
