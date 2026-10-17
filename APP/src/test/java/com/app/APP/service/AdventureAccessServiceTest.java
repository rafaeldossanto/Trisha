package com.app.APP.service;

import com.app.APP.entity.Adventure;
import com.app.APP.entity.Path;
import com.app.APP.model.enums.AdventureVisibility;
import com.app.APP.repository.AdventureParticipantRepository;
import com.app.APP.repository.PathRepository;
import com.app.APP.stub.AdventureStub;
import com.app.APP.stub.PathStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

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
    @Mock
    private PathRepository pathRepository;

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

    // ---- canViewPath ----

    @Test
    @DisplayName("canViewPath delegates to the path's adventure")
    void canViewPathDelegates() {
        Path path = PathStub.aPath()
                .adventure(adventure(AdventureVisibility.PUBLICA))
                .build();
        when(pathRepository.findById(PathStub.ID)).thenReturn(Optional.of(path));

        assertThat(service.canViewPath(OBSERVER, PathStub.ID)).isTrue();
    }

    @Test
    @DisplayName("canViewPath: the path's own owner always sees it, even in a PRIVADA adventure of someone else")
    void canViewPathOwnerAlwaysSees() {
        Path path = PathStub.aPath()
                .userId(OBSERVER)
                .adventure(adventure(AdventureVisibility.PRIVADA))
                .build();
        when(pathRepository.findById(PathStub.ID)).thenReturn(Optional.of(path));

        // O dono do caminho mantem acesso mesmo apos sair para uma aventura pessoal PRIVADA.
        assertThat(service.canViewPath(OBSERVER, PathStub.ID)).isTrue();
        verifyNoInteractions(participantRepository);
    }

    @Test
    @DisplayName("canViewPath is false for unknown path (does not leak existence)")
    void canViewPathUnknown() {
        when(pathRepository.findById("inexistente")).thenReturn(Optional.empty());

        assertThat(service.canViewPath(OBSERVER, "inexistente")).isFalse();
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
