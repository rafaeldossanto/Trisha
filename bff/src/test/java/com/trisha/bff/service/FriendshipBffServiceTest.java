package com.trisha.bff.service;

import com.trisha.bff.auth.AuthenticatedUser;
import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.response.FriendshipResponse;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FriendshipBffService")
class FriendshipBffServiceTest {

    @Mock
    private AppClient appClient;

    @InjectMocks
    private FriendshipBffService service;

    private final AuthenticatedUser user = new AuthenticatedUser(BffStub.USER_ID, "rafael#1", "rafael@trilha.com");
    private final Pageable pageable = PageRequest.of(0, 10);

    @Test
    @DisplayName("request deve delegar ao AppClient")
    void deveSolicitar() {
        when(appClient.requestFriendship(any())).thenReturn(BffStub.aFriendship());

        FriendshipResponse response = service.request(BffStub.aFriendshipRequest());

        assertThat(response.id()).isEqualTo("amizade-1");
        verify(appClient).requestFriendship(any());
    }

    @Test
    @DisplayName("respond deve delegar com o status")
    void deveResponder() {
        when(appClient.respondFriendship("amizade-1", "ACEITA")).thenReturn(BffStub.aFriendship());

        service.respond("amizade-1", "ACEITA");

        verify(appClient).respondFriendship("amizade-1", "ACEITA");
    }

    @Test
    @DisplayName("getPending deve delegar e retornar pagina")
    void deveListarPendentes() {
        PageResponse<FriendshipResponse> page = new PageResponse<>(List.of(BffStub.aFriendship()), 0, 10, 1L, 1);
        when(appClient.getPending(pageable)).thenReturn(page);

        PageResponse<FriendshipResponse> response = service.getPending(user, pageable);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("getFriends deve delegar e retornar pagina")
    void deveListarAmigos() {
        PageResponse<FriendshipResponse> page = new PageResponse<>(List.of(BffStub.aFriendship()), 0, 10, 1L, 1);
        when(appClient.getFriends(pageable)).thenReturn(page);

        PageResponse<FriendshipResponse> response = service.getFriends(user, pageable);

        assertThat(response.content()).hasSize(1);
    }
}
