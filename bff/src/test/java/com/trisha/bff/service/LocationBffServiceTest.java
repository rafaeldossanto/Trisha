package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.client.LocationClient;
import com.trisha.bff.model.dto.response.GpsPointResponse;
import com.trisha.bff.model.dto.response.LiveSessionResponse;
import com.trisha.bff.model.dto.response.SessionResponse;
import com.trisha.bff.model.dto.response.UserSummaryResponse;
import com.trisha.bff.stub.BffStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LocationBffService")
class LocationBffServiceTest {

    @Mock
    private LocationClient locationClient;
    @Mock
    private AppClient appClient;

    @InjectMocks
    private LocationBffService service;

    @Test
    @DisplayName("startSession deve delegar ao LocationClient")
    void deveIniciarSessao() {
        when(locationClient.startSession(any())).thenReturn(BffStub.aSession());

        SessionResponse response = service.startSession(BffStub.aSessionRequest());

        assertThat(response.id()).isEqualTo(BffStub.SESSION_ID);
        verify(locationClient).startSession(any());
    }

    @Test
    @DisplayName("registerPoint deve delegar ao LocationClient")
    void deveRegistrarPonto() {
        when(locationClient.registerPoint(any())).thenReturn(BffStub.aGpsPoint());

        GpsPointResponse response = service.registerPoint(BffStub.aGpsPointRequest());

        assertThat(response.order()).isEqualTo(1);
        verify(locationClient).registerPoint(any());
    }

    @Test
    @DisplayName("finishSession deve delegar ao LocationClient")
    void deveFinalizarSessao() {
        when(locationClient.finishSession(BffStub.SESSION_ID)).thenReturn(BffStub.aSession());

        service.finishSession(BffStub.SESSION_ID);

        verify(locationClient).finishSession(BffStub.SESSION_ID);
    }

    @Test
    @DisplayName("cancelSession deve delegar ao LocationClient")
    void deveCancelarSessao() {
        when(locationClient.cancelSession(BffStub.SESSION_ID)).thenReturn(BffStub.aSession());

        service.cancelSession(BffStub.SESSION_ID);

        verify(locationClient).cancelSession(BffStub.SESSION_ID);
    }

    @Test
    @DisplayName("updateVisibility deve delegar ao LocationClient")
    void deveAlterarVisibilidade() {
        when(locationClient.updateSessionVisibility(BffStub.SESSION_ID, "SEGUIDORES"))
                .thenReturn(BffStub.aSession());

        SessionResponse response = service.updateVisibility(BffStub.SESSION_ID, "SEGUIDORES");

        assertThat(response.id()).isEqualTo(BffStub.SESSION_ID);
        verify(locationClient).updateSessionVisibility(BffStub.SESSION_ID, "SEGUIDORES");
    }

    @Test
    @DisplayName("getSessionByPath deve delegar ao LocationClient")
    void deveBuscarSessaoPorCaminho() {
        when(locationClient.getSessionByPath(BffStub.PATH_ID)).thenReturn(BffStub.aSession());

        SessionResponse response = service.getSessionByPath(BffStub.PATH_ID);

        assertThat(response.pathId()).isEqualTo(BffStub.PATH_ID);
    }

    @Test
    @DisplayName("getLiveSessions deve enriquecer com o nome/codigo do trilheiro")
    void deveListarSessoesAoVivo() {
        LiveSessionResponse live = new LiveSessionResponse(
                BffStub.SESSION_ID, BffStub.PATH_ID, "outro-usuario", null, null,
                "PUBLICO", null, -20.43, -41.79);
        when(locationClient.getLiveSessions()).thenReturn(List.of(live));
        when(appClient.getUserSummaries(List.of("outro-usuario")))
                .thenReturn(List.of(new UserSummaryResponse("outro-usuario", "Ana", "ana#1")));

        List<LiveSessionResponse> response = service.getLiveSessions();

        assertThat(response).hasSize(1);
        assertThat(response.get(0).sessionId()).isEqualTo(BffStub.SESSION_ID);
        assertThat(response.get(0).userName()).isEqualTo("Ana");
        assertThat(response.get(0).userCode()).isEqualTo("ana#1");
    }

    @Test
    @DisplayName("getPointsBySession libera o catch-up de sessao ao vivo que o usuario pode assistir")
    void deveListarPontosPorSessao() {
        when(locationClient.getSession(BffStub.SESSION_ID)).thenReturn(BffStub.aSession());
        when(locationClient.canWatchSession(BffStub.SESSION_ID)).thenReturn(true);
        when(locationClient.getPointsBySession(BffStub.SESSION_ID)).thenReturn(List.of(BffStub.aGpsPoint()));

        List<GpsPointResponse> response = service.getPointsBySession(BffStub.USER_ID, BffStub.SESSION_ID);

        assertThat(response).hasSize(1);
        verify(appClient, never()).canViewPath(any());
    }

    @Test
    @DisplayName("getPointsBySession nega quando nem o ao vivo nem a aventura liberam")
    void deveNegarPontosDeSessaoSemAcesso() {
        when(locationClient.getSession(BffStub.SESSION_ID)).thenReturn(BffStub.aSession());
        when(locationClient.canWatchSession(BffStub.SESSION_ID)).thenReturn(false);
        when(appClient.canViewPath(BffStub.PATH_ID)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.getPointsBySession(BffStub.USER_ID, BffStub.SESSION_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");

        verify(locationClient, never()).getPointsBySession(any());
    }

    @Test
    @DisplayName("getPointsByPath delega quando o caminho e visivel ao observador")
    void deveListarPontosPorCaminho() {
        when(appClient.canViewPath(BffStub.PATH_ID)).thenReturn(true);
        when(locationClient.getPointsByPath(BffStub.PATH_ID)).thenReturn(List.of(BffStub.aGpsPoint()));

        List<GpsPointResponse> response = service.getPointsByPath(BffStub.USER_ID, BffStub.PATH_ID);

        assertThat(response).hasSize(1);
    }

    @Test
    @DisplayName("getPointsByPath nega caminho sem acesso")
    void deveNegarPontosDeCaminhoSemAcesso() {
        when(appClient.canViewPath(BffStub.PATH_ID)).thenReturn(false);

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> service.getPointsByPath(BffStub.USER_ID, BffStub.PATH_ID))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sem acesso");

        verify(locationClient, never()).getPointsByPath(any());
    }
}
