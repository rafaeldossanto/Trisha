package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.MediaRequest;
import com.app.APP.model.dto.response.MediaResponse;
import com.app.APP.model.enums.MediaType;
import com.app.APP.service.MediaService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = MediaController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("MediaController")
class MediaControllerTest {

    private static final String USER_ID = "usuario-1";
    private static final String ADVENTURE_ID = "aventura-1";
    private static final String MEDIA_ID = "midia-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MediaService mediaService;

    private MediaResponse responseStub() {
        return MediaResponse.builder()
                .id(MEDIA_ID).adventureId(ADVENTURE_ID)
                .type(MediaType.FOTO).url("https://storage.example.com/foto.jpg")
                .capturedAt(LocalDateTime.now()).build();
    }

    @Test
    @DisplayName("POST /midia salva midia com dados validos")
    void shouldSaveMedia() throws Exception {
        MediaRequest request = new MediaRequest(ADVENTURE_ID, null, MediaType.FOTO,
                "https://storage.example.com/foto.jpg", null, null, null, null);
        when(mediaService.save(any(), any(MediaRequest.class))).thenReturn(responseStub());

        mockMvc.perform(post("/midia")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(MEDIA_ID));
    }

    @Test
    @DisplayName("GET /midia/aventura/{adventureId} lista midias da aventura")
    void shouldListByAdventure() throws Exception {
        Page<MediaResponse> page = new PageImpl<>(List.of(responseStub()));
        when(mediaService.getByAdventure(eq(USER_ID), eq(ADVENTURE_ID), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/midia/aventura/{adventureId}", ADVENTURE_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(MEDIA_ID));
    }

    @Test
    @DisplayName("DELETE /midia/{id} retorna 403 quando nao e o dono")
    void shouldRejectDeleteByNonOwner() throws Exception {
        doThrow(new com.app.APP.exception.ForbiddenException("Voce nao e o dono desta midia"))
                .when(mediaService).delete(any(), eq(MEDIA_ID));

        mockMvc.perform(delete("/midia/{id}", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject("outro-usuario").claim("codigoUsuario", "code-outro"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("DELETE /midia/{id} exclui com sucesso")
    void shouldDeleteMedia() throws Exception {
        mockMvc.perform(delete("/midia/{id}", MEDIA_ID)
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk());
    }
}
