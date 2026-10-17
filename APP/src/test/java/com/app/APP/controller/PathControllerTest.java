package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.PathRequest;
import com.app.APP.model.dto.response.PathResponse;
import com.app.APP.model.enums.Color;
import com.app.APP.service.PathService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = PathController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("PathController")
class PathControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String ADVENTURE_ID = "aventura-1";
    private static final String PATH_ID = "caminho-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PathService pathService;

    private PathResponse responseStub() {
        return PathResponse.builder()
                .id(PATH_ID).adventureId(ADVENTURE_ID).userId(USER_ID)
                .color(Color.ROXO).number(1).startedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("POST /caminho inicia caminho com dados validos")
    void shouldStartPath() throws Exception {
        PathRequest request = new PathRequest(ADVENTURE_ID, Color.ROXO);
        when(pathService.start(any(), any(PathRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/caminho")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(PATH_ID))
                .andExpect(jsonPath("$.aventuraId").value(ADVENTURE_ID));
    }

    @Test
    @DisplayName("POST /caminho retorna 400 quando aventura nao encontrada")
    void shouldReturn400WhenAdventureNotFound() throws Exception {
        PathRequest request = new PathRequest("inexistente", Color.ROXO);
        when(pathService.start(any(), any(PathRequest.class)))
                .thenThrow(new IllegalArgumentException("Aventura nao encontrada"));

        mockMvc.perform(post("/caminho")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH /caminho/{id}/finalizar finaliza caminho")
    void shouldFinishPath() throws Exception {
        PathResponse finished = PathResponse.builder()
                .id(PATH_ID).adventureId(ADVENTURE_ID).userId(USER_ID)
                .color(Color.ROXO).number(1).startedAt(LocalDateTime.now())
                .finishedAt(LocalDateTime.now()).totalDistanceKm(12.5).build();
        when(pathService.finish(eq(USER_ID), eq(PATH_ID), eq(12.5))).thenReturn(finished);

        mockMvc.perform(patch("/caminho/{id}/finalizar", PATH_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("totalDistanceKm", "12.5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.distanciaTotalKm").value(12.5));
    }

    @Test
    @DisplayName("GET /caminho/aventura/{adventureId} lista caminhos da aventura")
    void shouldListByAdventure() throws Exception {
        Page<PathResponse> page = new PageImpl<>(List.of(responseStub()));
        when(pathService.getByAdventure(eq(USER_ID), eq(ADVENTURE_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/caminho/aventura/{adventureId}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(PATH_ID));
    }
}
