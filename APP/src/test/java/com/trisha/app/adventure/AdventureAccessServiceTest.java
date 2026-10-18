package com.trisha.app.adventure;

import com.trisha.app.stub.AdventureStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdventureAccessService")
class AdventureAccessServiceTest {

    private static final String OBSERVER = "observador-1";

    @Mock
    private AdventureParticipantRepository participantRepository;

    @InjectMocks
    private AdventureAccessService service;

    private Adventure adventure(AdventureVisibility visibility) {
        return AdventureStub.anAdventure().visibility(visibility).build();
    }

    // ---- canView ----

    @Test
    @DisplayName("canView: owner always sees, regardless of visibility")
    void ownerAlwaysSees() {
        Adventure privada = adventure(AdventureVisibility.PRIVADA);

        assertThat(service.canView(AdventureStub.USER_ID, privada)).isTrue();
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("canView: PUBLICA is visible to anyone")
    void publicVisibleToAnyone() {
        assertThat(service.canView(OBSERVER, adventure(AdventureVisibility.PUBLICA))).isTrue();
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("canView: SO_GRUPO visible only to participants")
    void groupOnlyForParticipants() {
        Adventure grupo = adventure(AdventureVisibility.SO_GRUPO);
        when(participantRepository.existsByAdventureIdAndUserId(AdventureStub.ID, OBSERVER))
                .thenReturn(true).thenReturn(false);

        assertThat(service.canView(OBSERVER, grupo)).isTrue();
        assertThat(service.canView(OBSERVER, grupo)).isFalse();
    }

    @Test
    @DisplayName("canView: PRIVADA hidden from everyone but the owner")
    void privateHiddenFromOthers() {
        assertThat(service.canView(OBSERVER, adventure(AdventureVisibility.PRIVADA))).isFalse();
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("validateView throws without distinguishing missing from forbidden")
    void validateViewThrows() {
        assertThatThrownBy(() -> service.validateView(OBSERVER, adventure(AdventureVisibility.PRIVADA)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao encontrada ou sem acesso");
    }

    // ---- canContribute ----

    @Test
    @DisplayName("canContribute: owner and participants can write; strangers cannot")
    void contributeOwnerOrParticipant() {
        Adventure adventure = adventure(AdventureVisibility.PRIVADA);
        when(participantRepository.existsByAdventureIdAndUserId(AdventureStub.ID, OBSERVER))
                .thenReturn(true).thenReturn(false);

        assertThat(service.canContribute(AdventureStub.USER_ID, adventure)).isTrue();
        assertThat(service.canContribute(OBSERVER, adventure)).isTrue();
        assertThat(service.canContribute(OBSERVER, adventure)).isFalse();
    }

    @Test
    @DisplayName("validateContribute throws for strangers")
    void validateContributeThrows() {
        Adventure adventure = adventure(AdventureVisibility.PUBLICA);
        when(participantRepository.existsByAdventureIdAndUserId(AdventureStub.ID, OBSERVER))
                .thenReturn(false);

        // Visibilidade PUBLICA da leitura, nao escrita: quem nao participa nao contribui.
        assertThatThrownBy(() -> service.validateContribute(OBSERVER, adventure))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nao participa desta aventura");
    }
}
