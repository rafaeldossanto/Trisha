package com.trisha.app.adventure;

import com.trisha.app.adventure.dto.AdventureResponse;
import com.trisha.app.path.PathRepository;
import com.trisha.app.stub.AdventureStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdventureMetricsAssembler")
class AdventureMetricsAssemblerTest {

    private static final String ADV_ID = AdventureStub.ID;

    @Mock private AdventureParticipantRepository participantRepository;
    @Mock private PathRepository pathRepository;

    @InjectMocks
    private AdventureMetricsAssembler assembler;

    @Test
    @DisplayName("build: participants counted; duration null while no path finished")
    void buildNullDurationWhenNothingFinished() {
        Adventure adventure = AdventureStub.anAdventure().build();
        when(participantRepository.countByAdventureId(ADV_ID)).thenReturn(1L);
        when(pathRepository.findTimespansByAdventureIds(List.of(ADV_ID)))
                // List.<Object[]>of: com um unico array, o varargs inferiria
                // List<Object> de 3 elementos em vez de uma linha de projecao.
                .thenReturn(List.<Object[]>of(new Object[]{ADV_ID, LocalDateTime.now(), null}));

        AdventureResponse response = assembler.build(adventure);

        assertThat(response.participantsCount()).isEqualTo(1);
        assertThat(response.durationHours()).isNull();
    }

    @Test
    @DisplayName("build: duration is the wall-clock span from first start to last finish, in hours")
    void buildComputesDuration() {
        Adventure adventure = AdventureStub.anAdventure().build();
        LocalDateTime start = LocalDateTime.of(2026, 7, 21, 8, 0);
        LocalDateTime end = LocalDateTime.of(2026, 7, 21, 14, 30);
        when(participantRepository.countByAdventureId(ADV_ID)).thenReturn(3L);
        when(pathRepository.findTimespansByAdventureIds(List.of(ADV_ID)))
                .thenReturn(List.<Object[]>of(new Object[]{ADV_ID, start, end}));

        AdventureResponse response = assembler.build(adventure);

        assertThat(response.participantsCount()).isEqualTo(3);
        assertThat(response.durationHours()).isEqualTo(6.5);
    }

    @Test
    @DisplayName("buildPage: metrics come in batch; adventures absent from the count default to zero")
    void buildPageBatches() {
        Adventure a1 = AdventureStub.anAdventure().id("a1").build();
        Adventure a2 = AdventureStub.anAdventure().id("a2").build();
        Page<Adventure> page = new PageImpl<>(List.of(a1, a2));
        when(participantRepository.countByAdventureIds(List.of("a1", "a2")))
                .thenReturn(List.<Object[]>of(new Object[]{"a1", 2L}));
        when(pathRepository.findTimespansByAdventureIds(List.of("a1", "a2")))
                .thenReturn(List.of());

        Page<AdventureResponse> result = assembler.buildPage(page);

        assertThat(result.getContent().get(0).participantsCount()).isEqualTo(2);
        assertThat(result.getContent().get(1).participantsCount()).isZero();
    }
}
