package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.AdventureParticipant;
import com.app.APP.entity.Region;
import com.app.APP.model.dto.request.AdventureRequest;
import com.app.APP.model.dto.response.AdventureResponse;
import com.app.APP.model.enums.AdventureStatus;
import com.app.APP.repository.AdventureRepository;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.RegionRepository;
import com.app.APP.stub.AdventureStub;
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
    private com.app.APP.repository.FollowerRepository followerRepository;
    @Mock
    private AdventureAccessService accessService;
    @Mock
    private AdventureExitService adventureExitService;
    @Mock
    private com.app.APP.repository.PathRepository pathRepository;

    private AdventureService service;

    @BeforeEach
    void setup() {
        AdventureMetricsAssembler metricsAssembler =
                new AdventureMetricsAssembler(participantRepository, pathRepository);
        service = new AdventureService(adventureRepository, participantRepository, regionRepository,
                followerRepository, accessService, adventureExitService, metricsAssembler);
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
    @DisplayName("addParticipant should persist when owner invites a new participant")
    void shouldAddParticipant() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(AdventureStub.ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(AdventureStub.ID, "usuario-9"))
                .thenReturn(false);

        service.addParticipant(AdventureStub.USER_ID, AdventureStub.ID, "usuario-9");

        ArgumentCaptor<AdventureParticipant> captor = ArgumentCaptor.forClass(AdventureParticipant.class);
        verify(participantRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo("usuario-9");
    }

    @Test
    @DisplayName("addParticipant should fail when caller does not own the adventure")
    void shouldFailAddParticipantNotOwner() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(AdventureStub.ID)).thenReturn(Optional.of(adventure));

        // Sem o gate de dono, qualquer um se adicionaria a uma aventura SO_GRUPO
        // e ganharia acesso de leitura a ela.
        assertThatThrownBy(() -> service.addParticipant("intruso", AdventureStub.ID, "intruso"))
                .isInstanceOf(com.app.APP.exception.ForbiddenException.class)
                .hasMessageContaining("nao e o dono");

        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("addParticipant should fail when user is already participating")
    void shouldFailDuplicateParticipant() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(AdventureStub.ID)).thenReturn(Optional.of(adventure));
        when(participantRepository.existsByAdventureIdAndUserId(AdventureStub.ID, "usuario-9"))
                .thenReturn(true);

        assertThatThrownBy(() -> service.addParticipant(AdventureStub.USER_ID, AdventureStub.ID, "usuario-9"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ja participa");

        verify(participantRepository, never()).save(any());
    }

    @Test
    @DisplayName("delete should remove existing adventure")
    void shouldDelete() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(adventureRepository.findById(AdventureStub.ID)).thenReturn(Optional.of(adventure));

        service.delete(AdventureStub.USER_ID, AdventureStub.ID);

        verify(adventureExitService).deleteWithContent(adventure);
    }
}
