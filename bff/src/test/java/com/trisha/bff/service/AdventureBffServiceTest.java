package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.response.AdventureResponse;
import com.trisha.bff.model.dto.response.LeaveAdventureResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.stub.BffStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdventureBffService")
class AdventureBffServiceTest {

    @Mock
    private AppClient appClient;

    @InjectMocks
    private AdventureBffService service;

    @Test
    @DisplayName("create deve delegar ao AppClient")
    void deveCriar() {
        when(appClient.createAdventure(any())).thenReturn(BffStub.anAdventure());

        AdventureResponse response = service.create(BffStub.anAdventureRequest());

        assertThat(response.id()).isEqualTo(BffStub.ADVENTURE_ID);
        verify(appClient).createAdventure(any());
    }

    @Test
    @DisplayName("getDetail deve agregar aventura, caminhos e midias")
    void deveMontarDetalhe() {
        when(appClient.getAdventure(BffStub.ADVENTURE_ID)).thenReturn(BffStub.anAdventure());
        when(appClient.getPathsByAdventure(eq(BffStub.ADVENTURE_ID), any()))
                .thenReturn(new PageResponse<>(List.of(BffStub.aPath()), 0, 50, 1L, 1));
        when(appClient.getMediaByAdventure(eq(BffStub.ADVENTURE_ID), any()))
                .thenReturn(new PageResponse<>(List.of(BffStub.aMedia()), 0, 50, 1L, 1));

        var detail = service.getDetail(BffStub.USER_ID, BffStub.ADVENTURE_ID);

        assertThat(detail.adventure().id()).isEqualTo(BffStub.ADVENTURE_ID);
        assertThat(detail.paths()).hasSize(1);
        assertThat(detail.media()).hasSize(1);
    }

    @Test
    @DisplayName("getById deve delegar ao AppClient")
    void deveBuscarPorId() {
        when(appClient.getAdventure(BffStub.ADVENTURE_ID)).thenReturn(BffStub.anAdventure());

        AdventureResponse response = service.getById(BffStub.USER_ID, BffStub.ADVENTURE_ID);

        assertThat(response.destination()).isEqualTo("Pico da Bandeira");
    }

    @Test
    @DisplayName("getByUser deve delegar e retornar pagina")
    void deveListarPorUsuario() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<AdventureResponse> page =
                new PageResponse<>(List.of(BffStub.anAdventure()), 0, 10, 1L, 1);
        when(appClient.getAdventuresByUser(BffStub.USER_ID, pageable)).thenReturn(page);

        PageResponse<AdventureResponse> response = service.getByUser(BffStub.USER_ID, BffStub.USER_ID, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("updateStatus deve delegar ao AppClient")
    void deveAtualizarStatus() {
        when(appClient.updateAdventureStatus(BffStub.ADVENTURE_ID, "CONCLUIDA"))
                .thenReturn(BffStub.anAdventure());

        service.updateStatus(BffStub.ADVENTURE_ID, "CONCLUIDA");

        verify(appClient).updateAdventureStatus(BffStub.ADVENTURE_ID, "CONCLUIDA");
    }

    @Test
    @DisplayName("addParticipant deve delegar ao AppClient")
    void deveAdicionarParticipante() {
        service.addParticipant(BffStub.ADVENTURE_ID, "usuario-9");

        verify(appClient).addParticipant(BffStub.ADVENTURE_ID, "usuario-9");
    }

    @Test
    @DisplayName("delete deve delegar ao AppClient")
    void deveDeletar() {
        service.delete(BffStub.ADVENTURE_ID);

        verify(appClient).deleteAdventure(BffStub.ADVENTURE_ID);
    }

    @Test
    @DisplayName("leave deve delegar ao AppClient e devolver o resultado")
    void deveSair() {
        when(appClient.leaveAdventure(BffStub.ADVENTURE_ID, true))
                .thenReturn(new LeaveAdventureResponse("aventura-pessoal", 1, 0));

        LeaveAdventureResponse response = service.leave(BffStub.ADVENTURE_ID, true);

        assertThat(response.personalAdventureId()).isEqualTo("aventura-pessoal");
        verify(appClient).leaveAdventure(BffStub.ADVENTURE_ID, true);
    }

    @Test
    @DisplayName("kick deve delegar ao AppClient")
    void deveRemoverParticipante() {
        when(appClient.kickParticipant(BffStub.ADVENTURE_ID, "usuario-9"))
                .thenReturn(new LeaveAdventureResponse("aventura-pessoal", 2, 0));

        service.kick(BffStub.ADVENTURE_ID, "usuario-9");

        verify(appClient).kickParticipant(BffStub.ADVENTURE_ID, "usuario-9");
    }
}
