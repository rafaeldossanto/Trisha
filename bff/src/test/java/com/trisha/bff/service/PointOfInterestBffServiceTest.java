package com.trisha.bff.service;

import com.trisha.bff.client.AppClient;
import com.trisha.bff.model.dto.response.EvidenceResponse;
import com.trisha.bff.model.dto.response.PageResponse;
import com.trisha.bff.model.dto.response.PointOfInterestResponse;
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
@DisplayName("PointOfInterestBffService")
class PointOfInterestBffServiceTest {

    @Mock
    private AppClient appClient;

    @InjectMocks
    private PointOfInterestBffService service;

    @Test
    @DisplayName("create deve delegar ao AppClient")
    void deveCriar() {
        when(appClient.createPoint(any())).thenReturn(BffStub.aPoint());

        PointOfInterestResponse response = service.create(BffStub.aPointRequest());

        assertThat(response.id()).isEqualTo(BffStub.POINT_ID);
        verify(appClient).createPoint(any());
    }

    @Test
    @DisplayName("getById deve delegar ao AppClient")
    void deveBuscarPorId() {
        when(appClient.getPoint(BffStub.POINT_ID)).thenReturn(BffStub.aPoint());

        PointOfInterestResponse response = service.getById(BffStub.POINT_ID);

        assertThat(response.confidenceLevel()).isEqualTo(3);
    }

    @Test
    @DisplayName("getByPath deve delegar e retornar pagina")
    void deveListarPorCaminho() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<PointOfInterestResponse> page =
                new PageResponse<>(List.of(BffStub.aPoint()), 0, 10, 1L, 1);
        when(appClient.getPointsByPath(BffStub.PATH_ID, pageable)).thenReturn(page);

        PageResponse<PointOfInterestResponse> response = service.getByPath(BffStub.PATH_ID, pageable);

        assertThat(response.content()).hasSize(1);
    }

    @Test
    @DisplayName("addEvidence deve delegar ao AppClient")
    void deveAdicionarEvidencia() {
        when(appClient.addEvidence(any())).thenReturn(BffStub.anEvidence());

        EvidenceResponse response = service.addEvidence(BffStub.anEvidenceRequest());

        assertThat(response.validated()).isTrue();
        verify(appClient).addEvidence(any());
    }
}
