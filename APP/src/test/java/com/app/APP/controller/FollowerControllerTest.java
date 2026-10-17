package com.app.APP.controller;

import com.app.APP.auth.WebConfig;
import com.app.APP.model.dto.request.FollowRequest;
import com.app.APP.model.dto.response.CountersResponse;
import com.app.APP.model.dto.response.FollowStatusResponse;
import com.app.APP.model.dto.response.PublicUserResponse;
import com.app.APP.service.FollowerService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        value = FollowerController.class,
        excludeFilters = @Filter(type = FilterType.ASSIGNABLE_TYPE, classes = com.app.APP.auth.SecurityConfig.class)
)
@Import({MockSecurityConfig.class, WebConfig.class})
@DisplayName("FollowerController")
class FollowerControllerTest {

    private static final String USER_ID = "usuario-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FollowerService followerService;

    @Test
    @DisplayName("POST /seguidor segue usuario com sucesso")
    void shouldFollowUser() throws Exception {
        FollowRequest request = new FollowRequest("trilheiro99");

        mockMvc.perform(post("/seguidor")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /seguidor retorna 400 ao tentar seguir a si mesmo")
    void shouldReturn400WhenFollowingSelf() throws Exception {
        FollowRequest request = new FollowRequest("meu-codigo");
        doThrow(new IllegalArgumentException("Voce nao pode seguir a si mesmo"))
                .when(followerService).follow(any(), eq("meu-codigo"));

        mockMvc.perform(post("/seguidor")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /seguidor/seguidores lista seguidores de um usuario")
    void shouldListFollowers() throws Exception {
        Page<PublicUserResponse> page = new PageImpl<>(
                List.of(PublicUserResponse.builder().userCode("amigo-1").name("Amigo Um").build()));
        when(followerService.getFollowers(eq("trilheiro42"), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/seguidor/seguidores")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("codigo", "trilheiro42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].codigoUsuario").value("amigo-1"));
    }

    @Test
    @DisplayName("GET /seguidor/contadores retorna contadores de um usuario")
    void shouldGetCounters() throws Exception {
        when(followerService.counters("trilheiro42")).thenReturn(new CountersResponse(10, 5));

        mockMvc.perform(get("/seguidor/contadores")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("codigo", "trilheiro42"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.seguidores").value(10))
                .andExpect(jsonPath("$.seguindo").value(5));
    }

    @Test
    @DisplayName("GET /seguidor/status retorna status de seguimento")
    void shouldGetFollowStatus() throws Exception {
        when(followerService.status(any(), eq("trilheiro99")))
                .thenReturn(new FollowStatusResponse(true, false, false));

        mockMvc.perform(get("/seguidor/status")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("codigo", "trilheiro99"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /seguidor/segue responde a checagem por ids (visibilidade SEGUIDORES)")
    void shouldCheckFollowerByIds() throws Exception {
        when(followerService.isFollower("usuario-2", USER_ID)).thenReturn(true);

        mockMvc.perform(get("/seguidor/segue")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("seguidorId", "usuario-2")
                        .param("seguidoId", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").value(true));
    }

    @Test
    @DisplayName("GET /seguidor/segue recusa consulta sobre relacao alheia")
    void shouldRejectFollowerCheckForUnrelatedUsers() throws Exception {
        mockMvc.perform(get("/seguidor/segue")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1")))
                        .param("seguidorId", "outro-1")
                        .param("seguidoId", "outro-2"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /seguidor/seguindo-ids lista ids de quem o autenticado segue")
    void shouldListFollowingIds() throws Exception {
        when(followerService.followingIds(USER_ID)).thenReturn(List.of("usuario-2", "usuario-3"));

        mockMvc.perform(get("/seguidor/seguindo-ids")
                        .with(jwt().jwt(j -> j.subject(USER_ID).claim("codigoUsuario", "code-1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0]").value("usuario-2"));
    }
}
