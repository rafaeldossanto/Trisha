package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.response.MediaResponse;
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
@DisplayName("MediaBffService")
class MediaBffServiceTest {

    @Mock
    private AppClient appClient;

    @InjectMocks
    private MediaBffService service;

    @Test
    @DisplayName("save deve delegar ao AppClient")
    void deveSalvar() {
        when(appClient.saveMedia(any())).thenReturn(BffStub.aMedia());

        MediaResponse response = service.save(BffStub.aMediaRequest());

        assertThat(response.url()).isEqualTo("https://cdn/midia.jpg");
        verify(appClient).saveMedia(any());
    }

    @Test
    @DisplayName("getByAdventure deve delegar e retornar pagina")
    void deveListarPorAventura() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<MediaResponse> page =
                new PageResponse<>(List.of(BffStub.aMedia()), 0, 10, 1L, 1);
        when(appClient.getMediaByAdventure(BffStub.ADVENTURE_ID, pageable)).thenReturn(page);

        PageResponse<MediaResponse> response = service.getByAdventure(BffStub.USER_ID, BffStub.ADVENTURE_ID, pageable);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("getByPath deve delegar e retornar pagina")
    void deveListarPorCaminho() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<MediaResponse> page =
                new PageResponse<>(List.of(BffStub.aMedia()), 0, 10, 1L, 1);
        when(appClient.getMediaByPath(BffStub.PATH_ID, pageable)).thenReturn(page);

        PageResponse<MediaResponse> response = service.getByPath(BffStub.USER_ID, BffStub.PATH_ID, pageable);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("delete deve delegar ao AppClient")
    void deveDeletar() {
        service.delete("midia-1");

        verify(appClient).deleteMedia("midia-1");
    }
}
