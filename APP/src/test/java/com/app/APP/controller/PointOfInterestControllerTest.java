package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.PointOfInterestRequest;
import com.app.APP.model.dto.response.PointOfInterestResponse;
import com.app.APP.model.enums.PointType;
import com.app.APP.service.PointOfInterestService;
import com.app.APP.service.PointOfInterestStatusService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PointOfInterestController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("PointOfInterestController")
class PointOfInterestControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String PATH_ID = "caminho-1";
    private static final String POINT_ID = "ponto-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PointOfInterestService pointOfInterestService;

    /** O controller passou a receber tambem o service de status pessoal do ponto
     *  (NO_RADAR/NA_MIRA/CONQUISTADO + objetivo); sem o mock o contexto nao sobe. */
    @MockitoBean
    private PointOfInterestStatusService pointOfInterestStatusService;

    private PointOfInterestResponse responseStub() {
        return PointOfInterestResponse.builder()
                .id(POINT_ID).pathId(PATH_ID).userId(USER_ID)
                .type(PointType.CACHOEIRA).latitude(-20.4).longitude(-43.5)
                .confidenceLevel(1).createdAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("POST /ponto-interesse cria ponto com dados validos")
    void shouldCreatePoint() throws Exception {
        PointOfInterestRequest request = new PointOfInterestRequest(PATH_ID, PointType.CACHOEIRA, null, null, -20.4, -43.5);
        when(pointOfInterestService.create(any(), any(PointOfInterestRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/ponto-interesse")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(POINT_ID))
                .andExpect(jsonPath("$.tipo").value("CACHOEIRA"));
    }

    @Test
    @DisplayName("POST /ponto-interesse retorna 400 quando caminho nao encontrado")
    void shouldReturn400WhenPathNotFound() throws Exception {
        PointOfInterestRequest request = new PointOfInterestRequest("inexistente", PointType.CACHOEIRA, null, null, -20.4, -43.5);
        when(pointOfInterestService.create(any(), any(PointOfInterestRequest.class)))
                .thenThrow(new IllegalArgumentException("Caminho nao encontrado"));

        mockMvc.perform(post("/ponto-interesse")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /ponto-interesse/{id} retorna ponto existente")
    void shouldGetById() throws Exception {
        when(pointOfInterestService.getById(USER_ID, POINT_ID)).thenReturn(responseStub());

        mockMvc.perform(get("/ponto-interesse/{id}", POINT_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(POINT_ID));
    }

    @Test
    @DisplayName("GET /ponto-interesse/caminho/{pathId} lista pontos do caminho")
    void shouldListByPath() throws Exception {
        Page<PointOfInterestResponse> page = new PageImpl<>(List.of(responseStub()));
        when(pointOfInterestService.getByPath(eq(USER_ID), eq(PATH_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/ponto-interesse/caminho/{pathId}", PATH_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(POINT_ID));
    }
}
