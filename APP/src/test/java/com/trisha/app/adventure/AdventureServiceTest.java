package com.trisha.app.adventure;

import com.trisha.app.adventure.dto.AdventureRequest;
import com.trisha.app.adventure.dto.AdventureResponse;
import com.trisha.app.region.Region;
import com.trisha.app.region.RegionRepository;
import com.trisha.app.social.FollowerService;
import com.trisha.app.stub.AdventureStub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdventureService")
class AdventureServiceTest {

    @Mock
    private AdventureRepository adventureRepository;
    @Mock
    private AdventureParticipantRepository participantRepository;
    @Mock
    private RegionRepository regionRepository;
    @Mock
    private FollowerService followerService;
    @Mock
    private AdventureAccessService accessService;
    @Mock
    private com.trisha.app.path.PathRepository pathRepository;

    private AdventureService service;

    @BeforeEach
    void setup() {
        AdventureMetricsAssembler metricsAssembler =
                new AdventureMetricsAssembler(participantRepository, pathRepository);
        service = new AdventureService(adventureRepository, participantRepository, regionRepository,
                followerService, accessService, metricsAssembler);
    }

    @Test
    @DisplayName("create should persist adventure and the creator participant")
    void shouldCreateAdventureAndParticipant() {
        AdventureRequest request = AdventureStub.aRequest();
        Region region = AdventureStub.RegionStub.aRegion().build();
        Adventure saved = AdventureStub.anAdventure().build();

        when(regionRepository.findById(request.regionId())).thenReturn(Optional.of(region));
        when(adventureRepository.save(any(Adventure.class))).thenReturn(saved);

        AdventureResponse response = service.create(AdventureStub.USER_ID, request);

        assertThat(response.id()).isEqualTo(saved.getId());
        assertThat(response.destination()).isEqualTo(saved.getDestination());
        verify(adventureRepository).save(any(Adventure.class));
        verify(participantRepository).save(any(AdventureParticipant.class));
    }

    @Test
    @DisplayName("create should fail when region does not exist")
    void shouldFailWhenRegionNotFound() {
        AdventureRequest request = AdventureStub.aRequest();
        when(regionRepository.findById(request.regionId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(AdventureStub.USER_ID, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("regiao nao encontrada");

        verify(adventureRepository, never()).save(any());
        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("getById should return existing adventure")
    void shouldReturnById() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(AdventureStub.ID)).thenReturn(Optional.of(adventure));

        AdventureResponse response = service.getById(AdventureStub.USER_ID, AdventureStub.ID);

        assertThat(response.id()).isEqualTo(AdventureStub.ID);
    }

    @Test
    @DisplayName("getById should fail when adventure does not exist")
    void shouldFailGetByIdNotFound() {
        when(adventureRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(AdventureStub.USER_ID, "inexistente"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Aventura nao encontrada");
    }

    @Test
    @DisplayName("getByUser should map page of adventures")
    void shouldListByUser() {
        Pageable pageable = PageRequest.of(0, 10);
        when(adventureRepository.findByUserId(AdventureStub.USER_ID, pageable))
                .thenReturn(new PageImpl<>(List.of(AdventureStub.anAdventure().build())));

        Page<AdventureResponse> response = service.getByUser(AdventureStub.USER_ID, AdventureStub.USER_ID, pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).userId()).isEqualTo(AdventureStub.USER_ID);
    }

    @Test
    @DisplayName("updateStatus should change the status and save")
    void shouldUpdateStatus() {
        Adventure adventure = AdventureStub.anAdventure().status(AdventureStatus.PLANEJADA).build();
        when(adventureRepository.findById(AdventureStub.ID)).thenReturn(Optional.of(adventure));
        when(adventureRepository.save(any(Adventure.class))).thenAnswer(inv -> inv.getArgument(0));

        AdventureResponse response = service.updateStatus(AdventureStub.USER_ID, AdventureStub.ID, AdventureStatus.CONCLUIDA);

        assertThat(response.status()).isEqualTo(AdventureStatus.CONCLUIDA);
    }

    @Test
    @DisplayName("createPersonalCopy saves a PRIVATE copy owned by the user, with the user as participant")
    void shouldCreatePersonalCopy() {
        Adventure origin = AdventureStub.anAdventure()
                .status(AdventureStatus.CONCLUIDA).visibility(AdventureVisibility.PUBLICA).build();
        when(adventureRepository.save(any(Adventure.class))).thenAnswer(inv -> inv.getArgument(0));

        Adventure personal = service.createPersonalCopy(origin, "membro-2");

        assertThat(personal.getId()).isNotNull().isNotEqualTo(origin.getId());
        assertThat(personal.getUserId()).isEqualTo("membro-2");
        assertThat(personal.getVisibility()).isEqualTo(AdventureVisibility.PRIVADA);
        assertThat(personal.getDestination()).isEqualTo(origin.getDestination());
        assertThat(personal.getStatus()).isEqualTo(AdventureStatus.CONCLUIDA);
        assertThat(personal.getRegion()).isNull();
        ArgumentCaptor<AdventureParticipant> captor = ArgumentCaptor.forClass(AdventureParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("membro-2");
        assertThat(captor.getValue().getAdventure()).isSameAs(personal);
    }

    @Test
    @DisplayName("getFeed should list own adventures plus the ones of followed users")
    void shouldBuildFeedFromFollowedUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        when(followerService.followingIds(AdventureStub.USER_ID)).thenReturn(List.of("seguido-1"));
        when(adventureRepository.findFeed(AdventureStub.USER_ID, List.of("seguido-1"), pageable))
                .thenReturn(new PageImpl<>(List.of(AdventureStub.anAdventure().build())));

        Page<AdventureResponse> response = service.getFeed(AdventureStub.USER_ID, pageable);

        assertThat(response.getContent()).hasSize(1);
    }

    @Test
    @DisplayName("getFeed should keep the IN clause valid with a sentinel when following nobody")
    void shouldBuildFeedWithSentinel() {
        Pageable pageable = PageRequest.of(0, 10);
        when(followerService.followingIds(AdventureStub.USER_ID)).thenReturn(List.of());
        when(adventureRepository.findFeed(eq(AdventureStub.USER_ID), eq(List.of("__sem_seguidos__")), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of()));

        assertThat(service.getFeed(AdventureStub.USER_ID, pageable).getContent()).isEmpty();
    }

    @Test
    @DisplayName("listVisibleInRegion should page the region's adventures visible to the observer")
    void shouldListVisibleInRegion() {
        Pageable pageable = PageRequest.of(0, 10);
        when(adventureRepository.findVisibleInRegion(AdventureStub.REGION_ID, "observador-1", pageable))
                .thenReturn(new PageImpl<>(List.of(AdventureStub.anAdventure().build())));

        Page<AdventureResponse> response = service.listVisibleInRegion(AdventureStub.REGION_ID, "observador-1", pageable);

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).regionId()).isEqualTo(AdventureStub.REGION_ID);
    }

    @Test
    @DisplayName("unlinkRegion should unlink the region's adventures without deleting them")
    void shouldUnlinkRegion() {
        service.unlinkRegion(AdventureStub.REGION_ID);

        verify(adventureRepository).unlinkRegion(AdventureStub.REGION_ID);
        verify(adventureRepository, never()).delete(any());
    }
}
